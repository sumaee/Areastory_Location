package com.areastory.location.api.service.impl;

import com.areastory.location.api.service.ArticleService;
import com.areastory.location.db.entity.Article;
import com.areastory.location.db.entity.Location;
import com.areastory.location.db.repository.ArticleRepository;
import com.areastory.location.dto.common.ArticleKafkaDto;
import com.areastory.location.dto.common.LocationDto;
import com.areastory.location.dto.response.LocationResp;
import com.areastory.location.util.ObjectMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {
    private static final String DOSI = "dosi";
    private static final String SIGUNGU = "sigungu";
    private static final String DONGEUPMYEON = "dongeupmyeon";
    private final ArticleRepository articleRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapperUtil objectMapperUtil;

    @Override
    @Transactional
    public void addArticle(ArticleKafkaDto articleKafkaDto) {

        Location location = Location.builder()
                .dosi(articleKafkaDto.getDosi())
                .sigungu(articleKafkaDto.getSigungu())
                .dongeupmyeon(articleKafkaDto.getDongeupmyeon())
                .build();

        LocationDto DongeupmyeonDto = new LocationDto(articleKafkaDto.getDosi(), articleKafkaDto.getSigungu(), articleKafkaDto.getDongeupmyeon());
        putMap(DongeupmyeonDto, articleKafkaDto);

        LocationDto SigunguDto = new LocationDto(articleKafkaDto.getDosi(), articleKafkaDto.getSigungu());
        putMap(SigunguDto, articleKafkaDto);

        LocationDto DosiDto = new LocationDto(articleKafkaDto.getDosi());
        putMap(DosiDto, articleKafkaDto);

        Article article = Article.articleBuilder()
                .articleId(articleKafkaDto.getArticleId())
                .userId(articleKafkaDto.getUserId())
                .image(articleKafkaDto.getThumbnail())
                .dailyLikeCount(articleKafkaDto.getDailyLikeCount())
                .createdAt(articleKafkaDto.getCreatedAt())
                .location(location)
                .publicYn(articleKafkaDto.getPublicYn())
                .build();
        articleRepository.save(article);
    }

    @Override
    @Transactional
    public void updateArticle(ArticleKafkaDto articleKafkaDto) {
        //dosi, sigungu, dongeupmyeon 1위 확인
        LocationDto dongeupmyeonDto = new LocationDto(articleKafkaDto.getDosi(), articleKafkaDto.getSigungu(), articleKafkaDto.getDongeupmyeon());
        checkLikeCount(DONGEUPMYEON, dongeupmyeonDto, articleKafkaDto);

        //dosi, sigungu 1위 확인
        LocationDto sigunguDto = new LocationDto(articleKafkaDto.getDosi(), articleKafkaDto.getSigungu());
        checkLikeCount(SIGUNGU, sigunguDto, articleKafkaDto);

        //dosi 1위 확인
        LocationDto dosiDto = new LocationDto(articleKafkaDto.getDosi());
        checkLikeCount(DOSI, dosiDto, articleKafkaDto);

        Article article = articleRepository.findById(articleKafkaDto.getArticleId()).orElseThrow();
        article.setDailyLikeCount(articleKafkaDto.getDailyLikeCount());
        article.setPublicYn(articleKafkaDto.getPublicYn());
    }

    @Override
    @Transactional
    public void deleteArticle(ArticleKafkaDto articleKafkaDto) {
        articleRepository.deleteById(articleKafkaDto.getArticleId());
    }

    private void putMap(LocationDto locationDto, ArticleKafkaDto articleKafkaDto) {
        if (redisTemplate.opsForValue().get(objectMapperUtil.toString(locationDto)) == null) {
            LocationResp locationResp = new LocationResp(articleKafkaDto.getArticleId(),
                    articleKafkaDto.getThumbnail(),
                    articleKafkaDto.getDailyLikeCount(),
                    locationDto);

            redisTemplate.opsForValue()
                    .set(objectMapperUtil.toString(locationDto), objectMapperUtil.toString(locationResp));
        }
    }

    private void checkLikeCount(String type, LocationDto locationDto, ArticleKafkaDto articleKafkaDto) {
        LocationResp currRedisLocationResp = objectMapperUtil.toObject(redisTemplate.opsForValue().get(objectMapperUtil.toString(locationDto)), LocationResp.class).orElse(null);
        Long currLikeCount = articleRepository.findById(articleKafkaDto.getArticleId()).get().getDailyLikeCount();

        // 좋아요를 누른 경우라면
        if (articleKafkaDto.getDailyLikeCount() > currLikeCount) {
            // 해당 지역이 redis에 안올라가 있거나 현재 redis의 좋아요보다 더 높은 좋아요 수라면 redis 갱신
            if (currRedisLocationResp == null || articleKafkaDto.getDailyLikeCount() > currRedisLocationResp.getLikeCount()) {
                currRedisLocationResp = new LocationResp(
                        articleKafkaDto.getArticleId(),
                        articleKafkaDto.getThumbnail(),
                        articleKafkaDto.getDailyLikeCount(),
                        locationDto
                );

                redisTemplate.opsForValue().set(objectMapperUtil.toString(locationDto), objectMapperUtil.toString(currRedisLocationResp));
            }
        }
        //좋아요 취소를 누르고 현재 redis에 올라가있는 게시물의 좋아요 취소인 경우
        else if (currRedisLocationResp != null
                && Objects.equals(currRedisLocationResp.getArticleId(), articleKafkaDto.getArticleId())
                && currRedisLocationResp.getLikeCount() > articleKafkaDto.getDailyLikeCount()) {
            // 카프카로 넘어온 좋아요 수보다 더 많은 좋아요를 가진 게시물 찾기
            LocationResp changeLocationResp = articleRepository.getDailyLikeCountData(type, articleKafkaDto.getArticleId(), locationDto, articleKafkaDto.getDailyLikeCount());

            // changeLocationResp를 redis에 넣는데 없다면 현재 데이터 좋아요 수 1개 줄인것 넣기
            redisTemplate.opsForValue()
                    .set(objectMapperUtil.toString(locationDto),
                            objectMapperUtil.toString(Objects.requireNonNullElseGet(changeLocationResp,
                                    () -> new LocationResp(articleKafkaDto.getArticleId(), articleKafkaDto.getThumbnail(), articleKafkaDto.getDailyLikeCount(), locationDto))));

        }
    }
}

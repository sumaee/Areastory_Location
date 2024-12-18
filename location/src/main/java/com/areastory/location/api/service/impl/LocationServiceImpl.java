package com.areastory.location.api.service.impl;

import com.areastory.location.api.service.LocationService;
import com.areastory.location.db.entity.Article;
import com.areastory.location.db.repository.ArticleRepository;
import com.areastory.location.dto.common.LocationDto;
import com.areastory.location.dto.response.LocationResp;
import com.areastory.location.util.ObjectMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationServiceImpl implements LocationService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapperUtil objectMapperUtil;
    private final ArticleRepository articleRepository;


    @Override
    public List<LocationResp> getMapImages(List<LocationDto> locationList) {
        return locationList.stream()
                .map(LocationDto::toString)
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .map(value -> objectMapperUtil.toObject(value, LocationResp.class).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<LocationResp> getUserMapImages(Long userId, List<LocationDto> locationList) {
        LocationDto locationDto = locationList.get(0);
        if (locationDto.getDongeupmyeon() != null) {
            return articleRepository.getImage(locationList, userId).stream().map(this::toDongResp).collect(Collectors.toList());
        } else if (locationDto.getSigungu() != null) {
            return articleRepository.getImage(locationList, userId).stream().map(this::toSigunguResp).collect(Collectors.toList());
        } else {
            return articleRepository.getImage(locationList, userId).stream().map(this::toDosiResp).collect(Collectors.toList());
        }
    }

    private LocationResp toDongResp(Article article) {
        if (article == null) {
            return null;
        }

        return LocationResp.builder()
                .dosi(article.getDosi())
                .sigungu(article.getSigungu())
                .dongeupmyeon(article.getDongeupmyeon())
                .likeCount(article.getDailyLikeCount())
                .image(article.getImage())
                .articleId(article.getArticleId())
                .build();
    }

    private LocationResp toSigunguResp(Article article) {
        if (article == null) {
            return null;
        }
        return LocationResp.builder()
                .dosi(article.getDosi())
                .sigungu(article.getSigungu())
                .likeCount(article.getDailyLikeCount())
                .image(article.getImage())
                .articleId(article.getArticleId())
                .build();
    }

    private LocationResp toDosiResp(Article article) {
        if (article == null) {
            return null;
        }
        return LocationResp.builder()
                .dosi(article.getDosi())
                .likeCount(article.getDailyLikeCount())
                .image(article.getImage())
                .articleId(article.getArticleId())
                .build();
    }

}

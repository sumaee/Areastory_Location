package com.areastory.location.kafka;

import com.areastory.location.api.service.ArticleService;
import com.areastory.location.config.properties.KafkaProperties;
import com.areastory.location.dto.common.ArticleKafkaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleListener {
    private final ArticleService articleService;
    private final KafkaProperties kafkaProperties;

    @KafkaListener(id = "map-article", topics = "server-article", containerFactory = "articleContainerFactory")
    public void articleListener(ArticleKafkaDto articleKafkaDto) {
        String type = articleKafkaDto.getType();
        
        if (type.equals(kafkaProperties.getCommand().getInsert())) {
            articleService.addArticle(articleKafkaDto);
        } else if (type.equals(kafkaProperties.getCommand().getUpdate())) {
            articleService.updateArticle(articleKafkaDto);
        } else if (type.equals(kafkaProperties.getCommand().getDelete())) {
            articleService.deleteArticle(articleKafkaDto);
        }
    }
//    @KafkaListener(id = KafkaProperties.GROUP_NAME_ARTICLE, topics = KafkaProperties.TOPIC_ARTICLE, containerFactory = "articleContainerFactory")
//    public void articleListen(ArticleKafkaDto articleKafkaDto) {
//        switch (articleKafkaDto.getType()) {
//            case KafkaProperties.INSERT:
//                articleService.addArticle(articleKafkaDto);
//                break;
//            case KafkaProperties.UPDATE:
//                articleService.updateArticle(articleKafkaDto);
//                break;
//            case KafkaProperties.DELETE:
//                articleService.deleteArticle(articleKafkaDto);
//                break;
//        }
//    }
}

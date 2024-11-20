package com.areastory.location.kafka;

import com.areastory.location.api.service.ArticleService;
import com.areastory.location.config.properties.KafkaProperties;
import com.areastory.location.dto.common.ArticleKafkaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleListener {
    private final ArticleService articleService;
    private final KafkaProperties kafkaProperties;

    @KafkaListener(id = "${kafka.group-name-article}", topics = "${kafka.topic-article}", containerFactory = "articleContainerFactory")
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
}

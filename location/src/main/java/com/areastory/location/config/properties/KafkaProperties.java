package com.areastory.location.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {
    private String kafkaUrl;
    private String topicArticle;
    private String groupNameArticle;
    private Command command;

    @Getter
    @Setter
    public static class Command {
        private String delete;
        private String update;
        private String insert;
    }

}

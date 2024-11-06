package com.areastory.location.kafka;

public interface KafkaProperties {
    String TOPIC_ARTICLE = "server-article";
    String GROUP_NAME_ARTICLE = "map-article";
    String KAFKA_URL = "host.docker.internal:9092";
    String DELETE = "delete";
    String UPDATE = "update";
    String INSERT = "insert";
}

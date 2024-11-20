package com.areastory.location.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class ObjectMapperUtil {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> String toString(T data) {
        String value;
        try {
            value = objectMapper.writeValueAsString(data);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return value;
    }

    public <T> Optional<T> toObject(String value, Class<T> classType) {
        try {
            return Optional.of(objectMapper.readValue(value, classType));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Optional.empty();
        }
    }
}

package com.rudraksha.shopsphere.shared.kafka.serialization;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

@Slf4j
public class JsonSerde {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    public static class JsonSerializer implements Serializer<Object> {

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
        }

        @Override
        public byte[] serialize(String topic, Object data) {
            if (data == null) {
                return null;
            }
            try {
                return OBJECT_MAPPER.writeValueAsBytes(data);
            } catch (Exception e) {
                log.error("Error serializing object for topic={}", topic, e);
                throw new RuntimeException("Error serializing object", e);
            }
        }

        @Override
        public void close() {
        }
    }

    public static class JsonDeserializer implements Deserializer<Object> {

        private Class<?> targetType = Object.class;

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            String className = (String) configs.get("value.deserializer.target.class");
            if (className != null) {
                try {
                    targetType = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    log.warn("Target class not found: {}, using Object.class", className);
                }
            }
        }

        @Override
        public Object deserialize(String topic, byte[] data) {
            if (data == null || data.length == 0) {
                return null;
            }
            try {
                return OBJECT_MAPPER.readValue(data, targetType);
            } catch (Exception e) {
                log.error("Error deserializing object from topic={}", topic, e);
                throw new RuntimeException("Error deserializing object", e);
            }
        }

        @Override
        public void close() {
        }
    }
}

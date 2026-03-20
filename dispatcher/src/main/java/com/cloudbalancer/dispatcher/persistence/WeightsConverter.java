package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

@Converter
public class WeightsConverter implements AttributeConverter<Map<String, Integer>, String> {

    private static final TypeReference<Map<String, Integer>> TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Integer> attribute) {
        try {
            return JsonUtil.mapper().writeValueAsString(attribute != null ? attribute : Map.of());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize weights", e);
        }
    }

    @Override
    public Map<String, Integer> convertToEntityAttribute(String dbData) {
        try {
            return new HashMap<>(JsonUtil.mapper().readValue(dbData, TYPE_REF));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize weights", e);
        }
    }
}

package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.common.model.ExecutionAttempt;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class ExecutionHistoryConverter implements AttributeConverter<List<ExecutionAttempt>, String> {

    private static final TypeReference<List<ExecutionAttempt>> TYPE_REF = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<ExecutionAttempt> attribute) {
        try {
            return JsonUtil.mapper().writeValueAsString(attribute != null ? attribute : List.of());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize execution history", e);
        }
    }

    @Override
    public List<ExecutionAttempt> convertToEntityAttribute(String dbData) {
        try {
            return new ArrayList<>(JsonUtil.mapper().readValue(dbData, TYPE_REF));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize execution history", e);
        }
    }
}

package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.common.model.WorkerCapabilities;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class WorkerCapabilitiesConverter implements AttributeConverter<WorkerCapabilities, String> {

    @Override
    public String convertToDatabaseColumn(WorkerCapabilities attribute) {
        try {
            return JsonUtil.mapper().writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize WorkerCapabilities", e);
        }
    }

    @Override
    public WorkerCapabilities convertToEntityAttribute(String dbData) {
        try {
            return JsonUtil.mapper().readValue(dbData, WorkerCapabilities.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize WorkerCapabilities", e);
        }
    }
}

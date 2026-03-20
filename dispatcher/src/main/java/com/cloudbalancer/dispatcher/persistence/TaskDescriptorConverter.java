package com.cloudbalancer.dispatcher.persistence;

import com.cloudbalancer.common.model.TaskDescriptor;
import com.cloudbalancer.common.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TaskDescriptorConverter implements AttributeConverter<TaskDescriptor, String> {

    @Override
    public String convertToDatabaseColumn(TaskDescriptor attribute) {
        try {
            return JsonUtil.mapper().writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize TaskDescriptor", e);
        }
    }

    @Override
    public TaskDescriptor convertToEntityAttribute(String dbData) {
        try {
            return JsonUtil.mapper().readValue(dbData, TaskDescriptor.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize TaskDescriptor", e);
        }
    }
}

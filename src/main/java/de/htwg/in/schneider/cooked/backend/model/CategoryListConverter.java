package de.htwg.in.schneider.cooked.backend.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CategoryListConverter implements AttributeConverter<List<Category>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Category>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<Category> attribute) {
        if (attribute == null) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize categories", e);
        }
    }

    @Override
    public List<Category> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = dbData.trim();
        if (!trimmed.startsWith("[")) {
            try {
                return List.of(Category.valueOf(trimmed));
            } catch (IllegalArgumentException e) {
                return Collections.emptyList();
            }
        }
        try {
            return MAPPER.readValue(trimmed, TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize categories", e);
        }
    }
}

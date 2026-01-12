package de.htwg.in.schneider.cooked.backend.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RecipeStepListConverter implements AttributeConverter<List<RecipeStep>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<RecipeStep>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<RecipeStep> attribute) {
        if (attribute == null) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize steps", e);
        }
    }

    @Override
    public List<RecipeStep> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize steps", e);
        }
    }
}

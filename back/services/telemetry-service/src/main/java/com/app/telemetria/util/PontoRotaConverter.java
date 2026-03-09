package com.app.telemetria.util;

import com.app.telemetria.entity.PontoRota;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class PontoRotaConverter implements AttributeConverter<List<PontoRota>, String> {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(List<PontoRota> attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            return "[]";
        }
    }
    
    @Override
    public List<PontoRota> convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, new TypeReference<List<PontoRota>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
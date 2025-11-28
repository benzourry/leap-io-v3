package com.benzourry.leap.utility;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Converter
public class LongListToStringConverter implements AttributeConverter<List<Long>, String> {

    @Override
    public String convertToDatabaseColumn(List<Long> attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        return String.join(",", attribute.stream().map(String::valueOf).toList());
    }

    @Override
    public List<Long> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return Collections.emptyList();
        List<Long> result = new ArrayList<>();
        for (String s : dbData.split(",")) {
            try { result.add(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
        }
        return result;
    }
}
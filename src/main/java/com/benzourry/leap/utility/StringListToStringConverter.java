package com.benzourry.leap.utility;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Converter
public class StringListToStringConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        return String.join(",", attribute.stream().map(String::valueOf).toList());
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String s : dbData.split(",")) {
            result.add(s);
        }
        return result;
    }
}
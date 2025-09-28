package com.tariffsheriff.backend.model.converter;

import jakarta.persistence.AttributeConverter;

/**
 * Base converter for enums that expose a getDbValue() and fromDb(String) API.
 */
public abstract class EnumStringConverter<E> implements AttributeConverter<E, String> {
    @Override
    public String convertToDatabaseColumn(E attribute) {
        if (attribute == null) return null;
        try {
            return (String) attribute.getClass().getMethod("getDbValue").invoke(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Enum converter requires getDbValue()", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            // static method on enum class
            return (E) getEnumType().getMethod("fromDb", String.class).invoke(null, dbData);
        } catch (Exception e) {
            throw new IllegalStateException("Enum converter requires static fromDb(String)", e);
        }
    }

    protected abstract Class<?> getEnumType();
}



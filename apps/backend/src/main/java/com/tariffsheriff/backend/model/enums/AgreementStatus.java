package com.tariffsheriff.backend.model.enums;

/**
 * Agreement status as stored in the database:
 * in_force | signed | inactive
 */
public enum AgreementStatus {
    IN_FORCE("in_force"),
    SIGNED("signed"),
    INACTIVE("inactive");

    private final String dbValue;

    AgreementStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static AgreementStatus fromDb(String value) {
        if (value == null) return null;
        for (AgreementStatus s : values()) {
            if (s.dbValue.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown AgreementStatus: " + value);
    }
}



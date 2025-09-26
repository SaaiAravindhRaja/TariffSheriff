package com.tariffsheriff.backend.model.enums;

/**
 * Tariff basis as stored in the database: MFN | PREF
 */
public enum TariffBasis {
    MFN("MFN"),
    PREF("PREF");

    private final String dbValue;

    TariffBasis(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static TariffBasis fromDb(String value) {
        if (value == null) return null;
        for (TariffBasis s : values()) {
            if (s.dbValue.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown TariffBasis: " + value);
    }
}



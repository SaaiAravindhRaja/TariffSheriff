package com.tariffsheriff.backend.model.enums;

/**
 * Tariff rate type as stored in the database: ad_valorem | specific | compound
 */
public enum TariffRateType {
    AD_VALOREM("ad_valorem"),
    SPECIFIC("specific"),
    COMPOUND("compound");

    private final String dbValue;

    TariffRateType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static TariffRateType fromDb(String value) {
        if (value == null) return null;
        for (TariffRateType s : values()) {
            if (s.dbValue.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown TariffRateType: " + value);
    }
}



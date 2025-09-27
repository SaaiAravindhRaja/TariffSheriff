package com.tariffsheriff.backend.model.converter;

import com.tariffsheriff.backend.model.enums.TariffRateType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TariffRateTypeConverter extends EnumStringConverter<TariffRateType> {
    @Override
    protected Class<?> getEnumType() {
        return TariffRateType.class;
    }
}



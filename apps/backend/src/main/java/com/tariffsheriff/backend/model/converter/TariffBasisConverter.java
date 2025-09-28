package com.tariffsheriff.backend.model.converter;

import com.tariffsheriff.backend.model.enums.TariffBasis;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TariffBasisConverter extends EnumStringConverter<TariffBasis> {
    @Override
    protected Class<?> getEnumType() {
        return TariffBasis.class;
    }
}



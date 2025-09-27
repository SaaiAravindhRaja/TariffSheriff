package com.tariffsheriff.backend.model.converter;

import com.tariffsheriff.backend.model.enums.AgreementStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AgreementStatusConverter extends EnumStringConverter<AgreementStatus> {
    @Override
    protected Class<?> getEnumType() {
        return AgreementStatus.class;
    }
}



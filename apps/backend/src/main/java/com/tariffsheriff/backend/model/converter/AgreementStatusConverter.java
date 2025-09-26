package com.tariffsheriff.backend.model.converter;

import com.tariffsheriff.backend.model.enums.AgreementStatus;

public class AgreementStatusConverter extends EnumStringConverter<AgreementStatus> {
    @Override
    protected Class<?> getEnumType() {
        return AgreementStatus.class;
    }
}



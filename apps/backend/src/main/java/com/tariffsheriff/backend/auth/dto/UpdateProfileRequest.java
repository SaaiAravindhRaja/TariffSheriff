package com.tariffsheriff.backend.auth.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String aboutMe;
}

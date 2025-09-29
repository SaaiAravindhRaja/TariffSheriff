package com.tariffsheriff.backend.user.dto;

import com.tariffsheriff.backend.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String aboutMe;
    private String role;
    private boolean admin;

    // Mapping method
    public static UserDto fromEntity(User user) {
        if (user == null) return null;

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .aboutMe(user.getAboutMe())
                .role(user.getRole())
                .admin(user.isAdmin())
                .build();
    }

   
}
package com.cource.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicProfileDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String idCard;
    private String role;
    private boolean isActive;

    private UserProfileDTO profile;
}

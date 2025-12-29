package com.cource.dto.user;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String idCard;
    private Long roleId;
    private boolean active = true;
}

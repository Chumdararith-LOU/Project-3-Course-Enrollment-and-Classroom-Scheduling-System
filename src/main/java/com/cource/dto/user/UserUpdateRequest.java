package com.cource.dto.user;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String idCard;
    private Long roleId;
    private Boolean active;
    private String password;
    private String dob;
    private String phone;
    private String bio;
    
}

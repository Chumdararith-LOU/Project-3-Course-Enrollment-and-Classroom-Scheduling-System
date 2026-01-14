package com.cource.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 1, message = "First name cannot be empty")
    private String firstName;

    @Size(min = 1, message = "Last name cannot be empty")
    private String lastName;

    private String idCard;
    private Long roleId;
    private Boolean active;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String dob;
    private String phone;
    private String bio;
}

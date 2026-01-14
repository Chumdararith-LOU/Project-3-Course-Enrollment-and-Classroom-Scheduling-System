package com.cource.service;

import com.cource.dto.auth.AuthResult;

public interface AuthService {
    AuthResult signIn(String email, String password);

    AuthResult signUpStudent(String firstName, String lastName, String email, String password);
}

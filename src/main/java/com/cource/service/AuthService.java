package com.cource.service;

import com.cource.dto.auth.AuthResult;

public interface AuthService {
    AuthResult signIn(String email, String password);
}

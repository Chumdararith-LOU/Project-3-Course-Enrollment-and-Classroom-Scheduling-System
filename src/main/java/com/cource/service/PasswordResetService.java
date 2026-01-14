package com.cource.service;

public interface PasswordResetService {
    /**
     * Creates and stores a reset token for the given email.
     * 
     * @return the generated token
     */
    String createResetToken(String email);

    void resetPassword(String token, String newPassword);
}

package com.cource.dto.auth;

/**
 * Simple result object for auth flows (sign-in / sign-up).
 * Controllers can decide how to apply the token (cookie/header) and where to redirect.
 */
public class AuthResult {
    private final Long userId;
    private final String roleCode;
    private final String token;
    private final String redirectUrl;

    public AuthResult(Long userId, String roleCode, String token, String redirectUrl) {
        this.userId = userId;
        this.roleCode = roleCode;
        this.token = token;
        this.redirectUrl = redirectUrl;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getToken() {
        return token;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }
}

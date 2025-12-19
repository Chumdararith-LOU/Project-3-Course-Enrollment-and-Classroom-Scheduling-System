package com.cource.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        
        String redirectUrl = determineTargetUrl(authentication);
        
        response.sendRedirect(redirectUrl);
    }
    
    private String determineTargetUrl(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                return "/admin/dashboard";
            } else if (authority.getAuthority().equals("ROLE_LECTURER")) {
                return "/lecturer/dashboard";
            } else if (authority.getAuthority().equals("ROLE_STUDENT")) {
                return "/student/dashboard";
            }
        }
        
        return "/";
    }
}
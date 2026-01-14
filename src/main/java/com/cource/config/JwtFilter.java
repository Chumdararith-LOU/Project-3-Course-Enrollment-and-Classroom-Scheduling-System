package com.cource.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtFilter(JwtService jwtService, @Qualifier("appUserDetailsService") UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // fallback to cookie
            if (request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if ("JWT".equals(c.getName())) {
                        token = c.getValue();
                    }
                }
            }
        }

        if (token != null) {
            try {
                Claims claims = jwtService.parseClaims(token);
                String username = claims.getSubject();
                log.info("JwtFilter: token parsed, subject={}", username);

                // Roles are stored as comma-separated string in the JWT
                List<String> roles = new ArrayList<>();
                Object rolesObj = claims.get("roles");
                if (rolesObj instanceof String) {
                    // Roles stored as comma-separated string
                    String rolesStr = (String) rolesObj;
                    if (rolesStr != null && !rolesStr.isEmpty()) {
                        for (String r : rolesStr.split(",")) {
                            roles.add(r.trim());
                        }
                    }
                } else if (rolesObj instanceof List) {
                    // Roles stored as list (fallback)
                    @SuppressWarnings("unchecked")
                    List<String> rolesList = (List<String>) rolesObj;
                    roles.addAll(rolesList);
                }
                log.info("JwtFilter: parsed roles={}", roles);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(token, userDetails.getUsername())) {
                        log.info("JwtFilter: token valid for user={}", userDetails.getUsername());
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        if (roles != null && !roles.isEmpty()) {
                            for (String r : roles) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                            }
                        }
                        // If the token did not contain roles or parsing failed to produce
                        // granted authorities, fall back to the authorities provided by
                        // UserDetails (DB-backed), preserving role-based access.
                        if (authorities.isEmpty() && userDetails != null) {
                            userDetails.getAuthorities()
                                    .forEach(a -> authorities.add(new SimpleGrantedAuthority(a.getAuthority())));
                        }
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails,
                                null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.info("JwtFilter: SecurityContext set for user={} authorities={}",
                                userDetails.getUsername(), authorities);
                    }
                }
            } catch (Exception ex) {
                log.warn("JwtFilter: invalid token - {}", ex.getMessage());
                // invalid token - ignore and continue
            }
        }

        filterChain.doFilter(request, response);
    }
}

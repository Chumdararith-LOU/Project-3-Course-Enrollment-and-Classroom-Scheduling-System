package com.cource.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    // Prefer app.jwt.* but fall back to legacy jwt.* keys used in
    // application.properties.
    @Value("${app.jwt.secret:${jwt.secret:secret-key-which-should-be-changed}}")
    private String secret;

    @Value("${app.jwt.expiration-ms:${jwt.expiration:86400000}}")
    private long expirationMs;

    private Key getSigningKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception ex) {
            // fallback to using raw bytes of secret string
            return Keys.hmacShaKeyFor(secret.getBytes());
        }
    }

    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("roles", String.join(",", roles))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, String username) {
        try {
            var claims = parseClaims(token);
            String sub = claims.getSubject();
            return sub != null && sub.equals(username) && claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

}
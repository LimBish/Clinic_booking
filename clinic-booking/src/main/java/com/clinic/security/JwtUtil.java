package com.clinic.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generate(UserDetails user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getAuthorities().iterator().next().getAuthority())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key())
                .compact();
    }

    public String extractEmail(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key()).build()
                    .parseClaimsJws(token).getBody().getSubject();
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException("expired");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("invalid");
        }
    }

    public boolean validate(String token, UserDetails user) {
        try {
            return extractEmail(token).equals(user.getUsername()) &&
                   !Jwts.parserBuilder().setSigningKey(key()).build()
                       .parseClaimsJws(token).getBody().getExpiration().before(new Date());
        } catch (RuntimeException e) { return false; }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) { super(message); }
    }

    public static class ExpiredTokenException extends RuntimeException {
        public ExpiredTokenException(String message) { super(message); }
    }
}

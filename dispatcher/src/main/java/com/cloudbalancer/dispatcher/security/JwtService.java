package com.cloudbalancer.dispatcher.security;

import com.cloudbalancer.common.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtService(
            @Value("${cloudbalancer.security.jwt-secret}") String secret,
            @Value("${cloudbalancer.security.access-token-expiration-seconds:900}") long accessExpSeconds,
            @Value("${cloudbalancer.security.refresh-token-expiration-seconds:604800}") long refreshExpSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.accessTokenExpirationSeconds = accessExpSeconds;
        this.refreshTokenExpirationSeconds = refreshExpSeconds;
    }

    public String generateAccessToken(String username, Role role) {
        return Jwts.builder()
            .subject(username)
            .claim("role", role.name())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationSeconds * 1000))
            .signWith(signingKey)
            .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Role extractRole(String token) {
        return Role.valueOf(parseClaims(token).get("role", String.class));
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

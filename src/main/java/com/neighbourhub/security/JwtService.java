package com.neighbourhub.security;

import com.neighbourhub.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * HS256 JWTs with the same claims the Node app used (id, role, name, email, 7 day expiry).
 * Tokens issued by the old backend verify here if JWT_SECRET is unchanged.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationDays;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-days:7}") long expirationDays) {
        // HS256 needs at least 32 bytes; a shorter secret fails fast at startup
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationDays = expirationDays;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("id", user.getId())
                .claim("role", user.getRole())
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationDays, ChronoUnit.DAYS)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Verifies signature + expiry and returns the "id" claim. Throws JwtException if invalid. */
    public String extractUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return claims.get("id", String.class);
    }
}

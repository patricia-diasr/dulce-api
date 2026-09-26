package com.dulce.backend.auth;

import com.dulce.backend.auth.dto.AuthResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public AuthResponse generateAuthResponse(
            String subjectEmail, Role role, Long user, String name) {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(expirationMinutes);

        String token =
                Jwts.builder()
                        .subject(subjectEmail)
                        .claim("role", role.name())
                        .claim("user", user)
                        .claim("name", name)
                        .issuedAt(new Date())
                        .expiration(Date.from(expiresAt.toInstant()))
                        .signWith(key)
                        .compact();

        return new AuthResponse(token, user, role.name(), name, expiresAt);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}

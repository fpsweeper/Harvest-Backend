package com.fpsweeper.harvest.security;

import com.fpsweeper.harvest.user.Users;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    // ✅ Must be at least 256 bits for HS256
    private static final String SECRET =
            "q8ZpR3xB7Kf6V2mE9yNwA5JcL0dUQH4sT1oXrIuMePbYgkFa";

    private static final long EXPIRATION = 86400000; // 24h

    private final SecretKey secretKey =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // ===============================
    // 1️⃣ Generate token
    // ===============================
    public String generateToken(Users user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(secretKey)
                .compact();
    }


    // ===============================
    // 2️⃣ Validate token
    // ===============================
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ===============================
    // 3️⃣ Parse token → Spring Authentication
    // ===============================
    public Authentication parseToken(String token) {
        Claims claims = extractAllClaims(token);

        String userId = claims.getSubject();
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);

        // ✅ Principal MUST be a Users object
        Users user = new Users();
        user.setId(UUID.fromString(userId));
        user.setEmail(email);
        user.setRole(role);

        // ✅ Authorities MUST exist
        List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

        // ✅ This creates an AUTHENTICATED token
        return new UsernamePasswordAuthenticationToken(
                user,          // principal
                null,          // credentials
                authorities    // authorities
        );
    }

    // ===============================
    // 4️⃣ Helpers
    // ===============================
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

package com.baber.apigateway.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

@Entity
@Table(name = "blacklisted_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistedToken {
    
    @Id
    @Column(length = 64) // SHA-256 hash length
    private String tokenHash;
    
    @Column(name = "token", length = 1000, nullable = false)
    private String token; // Full token stored separately
    
    @Column(name = "username", nullable = false)
    private String username;
    
    @Column(name = "blacklisted_at", nullable = false)
    private LocalDateTime blacklistedAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "reason")
    private String reason; // Optional reason for blacklisting
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // For soft deletion
    
    public BlacklistedToken(String token, String username, LocalDateTime expiresAt, String reason) {
        this.token = token;
        this.tokenHash = generateTokenHash(token);
        this.username = username;
        this.blacklistedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.reason = reason;
        this.isActive = true;
    }
    
    private String generateTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
} 
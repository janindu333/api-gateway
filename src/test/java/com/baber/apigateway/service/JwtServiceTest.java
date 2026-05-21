package com.baber.apigateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JWT validation configuration (no Keycloak network call).
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "issuerUri", "");
        ReflectionTestUtils.setField(jwtService, "jwksUri", "");
        ReflectionTestUtils.setField(jwtService, "acceptedIssuersCsv", "");
    }

    @Test
    void validateToken_returnsFalseWhenJwksAndIssuerAreBlank() {
        assertThat(jwtService.validateToken("dummy.jwt.token")).isFalse();
    }
}

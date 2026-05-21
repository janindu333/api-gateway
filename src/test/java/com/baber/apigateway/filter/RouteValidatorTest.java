package com.baber.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for public vs secured route matching (no Spring context).
 */
class RouteValidatorTest {

    private RouteValidator routeValidator;

    @BeforeEach
    void setUp() {
        routeValidator = new RouteValidator();
    }

    @Test
    void loginPathIsNotSecured() {
        assertSecured("/auth/login", false);
    }

    @Test
    void googleAuthPathIsNotSecured() {
        assertSecured("/auth/google/callback", false);
    }

    @Test
    void appointmentCreateIsSecured() {
        assertSecured("/api/appointment/create", true);
    }

    @Test
    void appointmentV1ListIsSecured() {
        assertSecured("/api/v1/appointments", true);
    }

    @Test
    void swaggerApiDocsIsNotSecured() {
        assertSecured("/booking-service/v3/api-docs", false);
    }

    @Test
    void adminLocationIsSecured() {
        assertSecured("/auth/location", true);
    }

    @Test
    void paymentWebhookIsNotSecured() {
        assertSecured("/api/payment/webhook/stripe", false);
    }

    private void assertSecured(String path, boolean expectedSecured) {
        Boolean secured = routeValidator
                .isSecured(MockServerHttpRequest.get(path).build())
                .block();
        assertThat(secured).isEqualTo(expectedSecured);
    }
}

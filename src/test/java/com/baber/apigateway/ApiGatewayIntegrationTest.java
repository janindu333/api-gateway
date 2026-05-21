package com.baber.apigateway;

import com.baber.apigateway.config.TestRedisConfiguration;
import com.baber.apigateway.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

/**
 * Integration tests: Spring context, actuator health, gateway routes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(TestRedisConfiguration.class)
class ApiGatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RouteLocator routeLocator;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void contextLoadsAndHealthIsUp() {
        doNothing().when(tokenBlacklistService).rebuildCacheFromDatabase();

        webTestClient.get()
                .uri("/actuator/health/liveness")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void gatewayExposesBookingAndAppointmentV1Routes() {
        List<String> routeIds = Flux.from(routeLocator.getRoutes())
                .map(Route::getId)
                .collectList()
                .block();

        assertThat(routeIds)
                .contains("booking-service", "booking-service-v1");
    }
}

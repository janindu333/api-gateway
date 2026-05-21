package com.baber.apigateway;

import com.baber.apigateway.config.TestRedisConfiguration;
import com.baber.apigateway.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

/**
 * Verifies k8s-style tracing properties from the observability feature load correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(TestRedisConfiguration.class)
class ObservabilityK8sPropertiesIntegrationTest {

    @DynamicPropertySource
    static void observabilityProperties(DynamicPropertyRegistry registry) {
        registry.add("management.tracing.enabled", () -> "true");
        registry.add("management.tracing.sampling.probability", () -> "1.0");
        registry.add("management.otlp.tracing.endpoint",
                () -> "http://tempo.observability.svc.cluster.local:4318/v1/traces");
        registry.add("management.tracing.propagation.type", () -> "w3c");
    }

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private Environment environment;

    @Test
    void tracingPropertiesAreApplied() {
        doNothing().when(tokenBlacklistService).rebuildCacheFromDatabase();

        assertThat(environment.getProperty("management.tracing.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("management.tracing.propagation.type")).isEqualTo("w3c");
        assertThat(environment.getProperty("management.otlp.tracing.endpoint"))
                .contains("tempo.observability.svc.cluster.local");
    }
}

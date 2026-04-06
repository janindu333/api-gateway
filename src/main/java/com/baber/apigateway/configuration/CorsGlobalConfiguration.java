package com.baber.apigateway.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
public class CorsGlobalConfiguration {

    /**
     * Comma-separated list of allowed origins, e.g.
     * cors.allowed-origins=http://localhost:3000,http://localhost:8080,http://127.0.0.1:8080
     */
    // Use Spring's standard key (`spring.web.cors.allowed-origins`) so Kubernetes config
    // and local config stay aligned.
    @Value("${spring.web.cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://127.0.0.1:8080}")
    private String corsAllowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        System.out.println("API-GATEWAY CORS corsAllowedOrigins runtime=" + corsAllowedOrigins);
        // Build an exact allow-list from `spring.web.cors.allowed-origins`.
        // Some origin matchers in this project appeared to ignore wildcard patterns,
        // so we register explicit origins for deterministic behavior.
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            Arrays.stream(corsAllowedOrigins.split(","))
                    .map(s -> s == null ? "" : s.trim())
                    .map(s -> s.replace("\"", "").replace("'", ""))
                    .filter(s -> !s.isBlank())
                    .forEach(origin -> {
                        if ("*".equals(origin)) {
                            corsConfig.addAllowedOriginPattern("*");
                        } else {
                            corsConfig.addAllowedOrigin(origin);
                        }
                    });
        } else {
            corsConfig.addAllowedOriginPattern("*");
        }

        corsConfig.setAllowCredentials(true);

        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
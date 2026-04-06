package com.baber.apigateway.configuration;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;

@Configuration
public class TracingExclusionConfig {

    @Bean
    ObservationPredicate actuatorExclusionPredicate() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext serverCtx) {
                String path = serverCtx.getCarrier().getPath().value();
                return !path.startsWith("/actuator");
            }
            return true;
        };
    }
}


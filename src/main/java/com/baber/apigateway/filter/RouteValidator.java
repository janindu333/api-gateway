package com.baber.apigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.util.List;
@Component
public class RouteValidator {
    public static final List<String> openApiEndpoints = List.of(
            "/auth/register",
            "/auth/login",
            "/permissions/**",
            "/roles/**",
            "/permissions/defaults/**",
            "/api/saloon/getNearBySaloons",
            "/eureka"
    );
    public Flux<Boolean> isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return Flux.fromIterable(openApiEndpoints)
                .map(uri -> !path.contains(uri));
    }
}

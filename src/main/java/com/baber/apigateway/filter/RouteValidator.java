package com.baber.apigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.List;

@Component
public class RouteValidator {
    public static final List<String> openApiEndpoints = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/getToken",
            "/auth/validate",
            "/auth/reset-password",
            "/auth/permissions/**",
            "/auth/roles/**",
            "/auth/permissions/defaults/**",
            "/api/saloon/getNearBySaloons",
            "/eureka",
            "/auth/roles",
            "/auth/admin/tokens/**" // Temporarily added for testing
    );
    
    // Secured endpoints that require JWT authentication
    public static final List<String> securedEndpoints = List.of(
            "/auth/location",
            "/auth/admin",
            "/auth/admin/tokens/**" // Token blacklist endpoints in Identity Service
    );
    
    public Mono<Boolean> isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        
        // First check if it's explicitly a secured endpoint
        boolean isExplicitlySecured = securedEndpoints.stream()
                .anyMatch(securedEndpoint -> {
                    if (securedEndpoint.endsWith("/**")) {
                        String basePath = securedEndpoint.substring(0, securedEndpoint.length() - 2);
                        return path.startsWith(basePath);
                    } else {
                        return path.equals(securedEndpoint);
                    }
                });
        
        if (isExplicitlySecured) {
            return Mono.just(true); // Explicitly secured
        }
        
        // Check if the path matches any open endpoint
        boolean isOpenEndpoint = openApiEndpoints.stream()
                .anyMatch(openEndpoint -> {
                    if (openEndpoint.endsWith("/**")) {
                        // Handle wildcard endpoints
                        String basePath = openEndpoint.substring(0, openEndpoint.length() - 2);
                        return path.startsWith(basePath);
                    } else {
                        // Exact match
                        return path.equals(openEndpoint);
                    }
                });
        
        // Return true if it's NOT an open endpoint (i.e., it's secured)
        return Mono.just(!isOpenEndpoint);
    }
}

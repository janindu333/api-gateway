package com.baber.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Ensure CORS headers are present on actual (non-OPTIONS) responses too.
 *
 * Some browsers will report a CORS error on the POST response even when preflight
 * succeeded, if Access-Control-Allow-Origin / Allow-Credentials are missing on the
 * actual response. This filter guarantees those headers are present whenever the
 * request has an Origin header and that Origin is allow-listed.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CorsResponseGlobalFilter implements GlobalFilter {

    private final Set<String> allowedOrigins;
    private final boolean allowCredentials;

    public CorsResponseGlobalFilter(
            @Value("${spring.web.cors.allowed-origins:}") String corsAllowedOrigins,
            @Value("${spring.web.cors.allow-credentials:false}") boolean allowCredentials
    ) {
        this.allowCredentials = allowCredentials;
        this.allowedOrigins = parseAllowedOrigins(corsAllowedOrigins);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String origin = request.getHeaders().getOrigin();
        if (origin == null || origin.isBlank()) {
            return chain.filter(exchange);
        }

        // If allow list is empty, do nothing (we don't want to accidentally allow all).
        if (allowedOrigins.isEmpty()) {
            return chain.filter(exchange);
        }

        // Only echo back exact allowed origins.
        if (!allowedOrigins.contains(origin)) {
            return chain.filter(exchange);
        }

        ServerHttpResponse response = exchange.getResponse();
        response.beforeCommit(() -> {
            HttpHeaders headers = response.getHeaders();
            if (!headers.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                headers.add(HttpHeaders.VARY, "Origin");
            }
            if (allowCredentials) {
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            }
            return Mono.empty();
        });

        return chain.filter(exchange);
    }

    private static Set<String> parseAllowedOrigins(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<String> out = new HashSet<>();
        Arrays.stream(value.split(","))
                .map(s -> s == null ? "" : s.trim())
                .map(s -> s.replace("\"", "").replace("'", ""))
                .filter(s -> !s.isBlank())
                .forEach(out::add);
        return out;
    }
}


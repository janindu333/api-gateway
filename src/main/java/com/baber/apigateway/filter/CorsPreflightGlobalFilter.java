package com.baber.apigateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;

/**
 * Force CORS preflight success with explicit Access-Control-Allow-* headers.
 *
 * Without this, Spring's reactive CORS handling in this project returns 200
 * for some origins but does not include Access-Control-Allow-Origin, which
 * causes Swagger UI "Failed to fetch / CORS" even when the network request is OK.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsPreflightGlobalFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String origin = request.getHeaders().getOrigin();
        if (origin != null && !origin.isBlank()) {
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.getHeaders().set(HttpHeaders.VARY, "Origin");

            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                "GET,POST,PUT,DELETE,OPTIONS,PATCH");

        String requestedHeaders = request.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        if (requestedHeaders != null && !requestedHeaders.isBlank()) {
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, requestedHeaders);
        } else {
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        }

        response.setStatusCode(HttpStatus.OK);
        return response.setComplete();
    }
}


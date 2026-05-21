package com.baber.apigateway.filter;

import com.baber.apigateway.dto.ErrorResponse;
import com.baber.apigateway.service.JwtService;
import com.baber.apigateway.service.TokenBlacklistService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {
    @Autowired
    public RouteValidator validator;

    @Autowired
    private JwtService jwtUtil;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    public AuthenticationFilter() {
        super(Config.class);
    }
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return validator.isSecured(exchange.getRequest())
                    .flatMap(isSecured -> {
                        if (!isSecured) {
                            return chain.filter(exchange);
                        }

                        String originalAuthHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                        if (originalAuthHeader == null || originalAuthHeader.isBlank()) {
                            return handleErrorResponse(exchange, "missing authorization header", HttpStatus.UNAUTHORIZED);
                        }

                        if (!originalAuthHeader.startsWith("Bearer ")) {
                            return handleErrorResponse(exchange, "invalid authorization header", HttpStatus.UNAUTHORIZED);
                        }

                        String token = originalAuthHeader.substring(7).trim();
                        if (token.isEmpty()) {
                            return handleErrorResponse(exchange, "invalid authorization header", HttpStatus.UNAUTHORIZED);
                        }

                        if (tokenBlacklistService.isBlacklisted(token)) {
                            return handleErrorResponse(exchange, "Token is invalidated", HttpStatus.UNAUTHORIZED);
                        }

                        if (!jwtUtil.validateToken(token)) {
                            return handleErrorResponse(exchange, "unauthorized access to application", HttpStatus.UNAUTHORIZED);
                        }

                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header(HttpHeaders.AUTHORIZATION, originalAuthHeader)
                                .build();
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });
        };
    }
    
    private Mono<Void> handleErrorResponse(ServerWebExchange exchange, String errorMessage, HttpStatus status) {
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                "Fail",
                0,
                status.getReasonPhrase(),
                null,
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                exchange.getRequest().getPath().value(),
                status.value(),
                status.getReasonPhrase(),
                UUID.randomUUID().toString(),
                errorMessage
        );

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] response = objectMapper.writeValueAsBytes(errorResponse);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(response)))
                    .then(); // Ensure a Mono<Void> is returned
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
    public static class Config {

    }
}
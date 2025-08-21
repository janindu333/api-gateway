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
                        if (isSecured) {
                            // header contains token or not
                            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                                return handleErrorResponse(exchange, "missing authorization header",
                                        HttpStatus.UNAUTHORIZED);
                            }

                            String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION)
                                    .get(0);
                            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                                authHeader = authHeader.substring(7);
                            }
                            
                            // Check if token is blacklisted
                            if (tokenBlacklistService.isBlacklisted(authHeader)) {
                                return handleErrorResponse(exchange, "Token is invalidated",
                                        HttpStatus.UNAUTHORIZED);
                            }
                            
                            try {
                               jwtUtil.validateToken(authHeader);
                                // jwtUtil.extractClaims(authHeader);

                                // Debug: Log the token being forwarded
                                System.out.println("API Gateway: Forwarding token to downstream service");
                                System.out.println("API Gateway: Token = " + authHeader.substring(0, Math.min(50, authHeader.length())) + "...");
                                
                                // Forward the Authorization header to downstream services
                                String originalAuthHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                                System.out.println("API Gateway: Original Auth Header = " + originalAuthHeader);
                                System.out.println("API Gateway: All headers = " + exchange.getRequest().getHeaders());
                                
                                // Ensure the Authorization header is properly forwarded
                                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                    .header(HttpHeaders.AUTHORIZATION, originalAuthHeader)
                                    .build();
                                
                                ServerWebExchange mutatedExchange = exchange.mutate()
                                    .request(mutatedRequest)
                                    .build();
                                
                                return chain.filter(mutatedExchange);
                            } catch (Exception e) {
                                return handleErrorResponse(exchange, "unauthorized access to application",
                                        HttpStatus.UNAUTHORIZED);
                            }
                        } else {
                            // Route is not secured - allow the request to proceed
                            return chain.filter(exchange);  // ✅ Allow request to continue
                        }
                    })
                    .then(); // Ensure a Mono<Void> is returned at the end
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

package com.baber.apigateway.configuration;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class CorsGlobalConfiguration {

    @Bean
    public GatewayFilter corsFilter() {
        System.out.println("GatewayFilter executed");

        return (exchange, chain) -> {
            System.out.println("Request Headers: " + exchange.getRequest().getHeaders());

            exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Headers", "*");
            return chain.filter(exchange);
        };
    }
}

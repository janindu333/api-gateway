package com.baber.apigateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;

@Configuration
@ComponentScan(basePackages = {
    "com.baber.apigateway.service",
    "com.baber.apigateway.filter"
})
public class GatewayConfig {
    // This configuration class ensures all components are properly scanned
    // and available for dependency injection
}
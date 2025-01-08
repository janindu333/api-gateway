package com.baber.apigateway;

import com.baber.apigateway.configuration.ServiceLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
@EnableDiscoveryClient
@SpringBootApplication
public class ApiGatewayApplication {
    // Create an instance of ServiceLogger
    private static final ServiceLogger logger = new ServiceLogger(ApiGatewayApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
        // Log a message when the application starts
        logger.info("api gateway application started successfully.");
    }
}

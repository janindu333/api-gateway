package com.baber.apigateway.config;

import com.baber.apigateway.service.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupConfig implements CommandLineRunner {
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    
    @Override
    public void run(String... args) throws Exception {
        // Rebuild Redis cache from database on startup
        // This ensures no data loss after Redis restarts
        tokenBlacklistService.rebuildCacheFromDatabase();
        System.out.println("✅ Redis cache rebuilt from database successfully");
    }
} 
package com.baber.apigateway.service;

import com.baber.apigateway.entity.BlacklistedToken;
import com.baber.apigateway.repository.BlacklistedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class TokenBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;
    
    private static final String BLACKLIST_PREFIX = "blacklist:";
    
    /**
     * Add a token to the blacklist (Redis + Database)
     * Industry standard: Primary in Redis, backup in Database
     */
    public void blacklistToken(String token, String username, String reason, int expirationHours) {
        try {
            String key = BLACKLIST_PREFIX + token;
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(expirationHours);
            
            // 1. Store in Redis (primary - fast access)
            redisTemplate.opsForValue().set(key, "1", Duration.ofHours(expirationHours));
            
            // 2. Store in Database (backup - persistence + audit)
            BlacklistedToken blacklistedToken = new BlacklistedToken(token, username, expiresAt, reason);
            blacklistedTokenRepository.save(blacklistedToken);
            
            logger.info("Token blacklisted successfully for user: {}, reason: {}", username, reason);
            
        } catch (Exception e) {
            logger.error("Failed to blacklist token for user: {}", username, e);
            throw new RuntimeException("Failed to blacklist token", e);
        }
    }
    
    /**
     * Check if a token is blacklisted (Redis first, Database fallback)
     * Industry standard: Fast path with reliable fallback
     */
    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            
            // 1. Check Redis first (fast - microseconds)
            Boolean isInRedis = redisTemplate.hasKey(key);
            if (Boolean.TRUE.equals(isInRedis)) {
                logger.debug("Token found in Redis blacklist");
                return true;
            }
            
            // 2. Fallback to Database (slower but reliable)
            boolean isInDatabase = blacklistedTokenRepository.findByTokenAndIsActiveTrue(token).isPresent();
            if (isInDatabase) {
                logger.info("Token found in Database blacklist, adding to Redis");
                // Add back to Redis for future fast access
                redisTemplate.opsForValue().set(key, "1", Duration.ofHours(2));
            }
            
            return isInDatabase;
            
        } catch (Exception e) {
            logger.error("Error checking token blacklist status", e);
            // Fail secure - if we can't check, assume it's blacklisted
            return true;
        }
    }
    
    /**
     * Remove a token from blacklist (Redis + Database)
     */
    public void removeFromBlacklist(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            
            // 1. Remove from Redis
            redisTemplate.delete(key);
            
            // 2. Soft delete from Database
            blacklistedTokenRepository.findByTokenAndIsActiveTrue(token)
                    .ifPresent(blacklistedToken -> {
                        blacklistedToken.setActive(false);
                        blacklistedTokenRepository.save(blacklistedToken);
                        logger.info("Token removed from blacklist: {}", token);
                    });
                    
        } catch (Exception e) {
            logger.error("Failed to remove token from blacklist: {}", token, e);
            throw new RuntimeException("Failed to remove token from blacklist", e);
        }
    }
    
    /**
     * Get blacklist statistics for monitoring
     */
    public BlacklistStats getBlacklistStats() {
        try {
            long redisCount = redisTemplate.keys(BLACKLIST_PREFIX + "*").size();
            long databaseCount = blacklistedTokenRepository.countByIsActiveTrue();
            
            return new BlacklistStats(redisCount, databaseCount);
            
        } catch (Exception e) {
            logger.error("Failed to get blacklist statistics", e);
            return new BlacklistStats(0, 0);
        }
    }
    
    /**
     * Cleanup expired tokens (scheduled task)
     */
    public void cleanupExpiredTokens() {
        try {
            List<BlacklistedToken> expiredTokens = blacklistedTokenRepository.findExpiredTokens(LocalDateTime.now());
            
            for (BlacklistedToken token : expiredTokens) {
                // Remove from Redis
                redisTemplate.delete(BLACKLIST_PREFIX + token.getToken());
                
                // Soft delete from Database
                token.setActive(false);
                blacklistedTokenRepository.save(token);
            }
            
            logger.info("Cleaned up {} expired blacklisted tokens", expiredTokens.size());
            
        } catch (Exception e) {
            logger.error("Failed to cleanup expired tokens", e);
        }
    }
    
    /**
     * Rebuild Redis cache from database (startup recovery)
     */
    public void rebuildCacheFromDatabase() {
        try {
            List<BlacklistedToken> blacklistedTokens = blacklistedTokenRepository.findByIsActiveTrue();
            int rebuiltCount = 0;
            
            for (BlacklistedToken token : blacklistedTokens) {
                if (token.getExpiresAt().isAfter(LocalDateTime.now())) {
                    String key = BLACKLIST_PREFIX + token.getToken();
                    long hoursUntilExpiry = java.time.Duration.between(LocalDateTime.now(), token.getExpiresAt()).toHours();
                    redisTemplate.opsForValue().set(key, "1", Duration.ofHours(hoursUntilExpiry));
                    rebuiltCount++;
                }
            }
            
            logger.info("Rebuilt Redis cache with {} active blacklisted tokens", rebuiltCount);
            
        } catch (Exception e) {
            logger.error("Failed to rebuild Redis cache from database", e);
        }
    }
    
    /**
     * Statistics class for monitoring
     */
    public static class BlacklistStats {
        private final long redisCount;
        private final long databaseCount;
        
        public BlacklistStats(long redisCount, long databaseCount) {
            this.redisCount = redisCount;
            this.databaseCount = databaseCount;
        }
        
        public long getRedisCount() { return redisCount; }
        public long getDatabaseCount() { return databaseCount; }
        public long getTotalCount() { return Math.max(redisCount, databaseCount); }
    }
} 
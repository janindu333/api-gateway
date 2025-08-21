package com.baber.apigateway.repository;

import com.baber.apigateway.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {
    
    /**
     * Find all active blacklisted tokens
     */
    List<BlacklistedToken> findByIsActiveTrue();
    
    /**
     * Find all active blacklisted tokens for a specific user
     */
    List<BlacklistedToken> findByUsernameAndIsActiveTrue(String username);
    
    /**
     * Find a specific token by its value
     */
    Optional<BlacklistedToken> findByTokenAndIsActiveTrue(String token);
    
    /**
     * Find all expired tokens that should be cleaned up
     */
    @Query("SELECT bt FROM BlacklistedToken bt WHERE bt.expiresAt < :currentTime AND bt.isActive = true")
    List<BlacklistedToken> findExpiredTokens(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Count active blacklisted tokens
     */
    long countByIsActiveTrue();
    
    /**
     * Count active blacklisted tokens for a specific user
     */
    long countByUsernameAndIsActiveTrue(String username);
} 
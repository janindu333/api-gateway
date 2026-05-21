-- Database initialization script for token blacklisting
-- Run this script to create the required database and table

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS baber_gateway;

-- Use the database
USE baber_gateway;

-- Create blacklisted_tokens table with new structure
CREATE TABLE IF NOT EXISTS blacklisted_tokens (
    token_hash VARCHAR(64) PRIMARY KEY COMMENT 'SHA-256 hash of JWT token',
    token VARCHAR(1000) NOT NULL COMMENT 'Full JWT token value',
    username VARCHAR(255) NOT NULL COMMENT 'Username of blacklisted user',
    blacklisted_at DATETIME NOT NULL COMMENT 'When token was blacklisted',
    expires_at DATETIME NOT NULL COMMENT 'When token expires',
    reason VARCHAR(500) COMMENT 'Reason for blacklisting',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Soft deletion flag'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes separately to avoid key length issues
CREATE INDEX idx_username ON blacklisted_tokens(username);
CREATE INDEX idx_expires_at ON blacklisted_tokens(expires_at);
CREATE INDEX idx_is_active ON blacklisted_tokens(is_active);
CREATE INDEX idx_blacklisted_at ON blacklisted_tokens(blacklisted_at);
CREATE INDEX idx_token ON blacklisted_tokens(token(255)); -- Index first 255 chars of token

-- Insert sample data for testing (with SHA-256 hashes)
INSERT INTO blacklisted_tokens (token_hash, token, username, blacklisted_at, expires_at, reason, is_active) VALUES
('e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 'sample_token_1', 'test_user_1', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'Test blacklisting', TRUE),
('a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 'sample_token_2', 'test_user_2', NOW(), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'Security violation', TRUE);

-- Create user for API Gateway (if not exists)
CREATE USER IF NOT EXISTS 'gateway_user'@'localhost' IDENTIFIED BY 'gateway_password';
GRANT ALL PRIVILEGES ON baber_gateway.* TO 'gateway_user'@'localhost';
FLUSH PRIVILEGES;

-- Show table structure
DESCRIBE blacklisted_tokens;

-- Show sample data
SELECT token_hash, username, blacklisted_at, expires_at, reason, is_active FROM blacklisted_tokens; 
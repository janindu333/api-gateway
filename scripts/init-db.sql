-- =============================================================================
-- Database Initialization Script for Saloon Service
-- =============================================================================

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS saloon_service;
USE saloon_service;

-- Create user tables for Identity Service
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone_number VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_active (is_active)
);

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resource_action (resource, action)
);

-- Create user_roles junction table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Create role_permissions junction table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Create refresh_tokens table for JWT management
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_revoked BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
);

-- Create password_reset_tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_used BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id)
);

-- Insert default roles
INSERT IGNORE INTO roles (name, description) VALUES
('ADMIN', 'System Administrator with full access'),
('USER', 'Regular user with basic access'),
('SALOON_OWNER', 'Saloon owner with management access'),
('EMPLOYEE', 'Saloon employee with limited access');

-- Insert default permissions
INSERT IGNORE INTO permissions (name, description, resource, action) VALUES
-- User management permissions
('USER_READ', 'Read user information', 'USER', 'READ'),
('USER_WRITE', 'Create and update users', 'USER', 'WRITE'),
('USER_DELETE', 'Delete users', 'USER', 'DELETE'),

-- Role management permissions
('ROLE_READ', 'Read role information', 'ROLE', 'READ'),
('ROLE_WRITE', 'Create and update roles', 'ROLE', 'WRITE'),
('ROLE_DELETE', 'Delete roles', 'ROLE', 'DELETE'),

-- Saloon management permissions
('SALOON_READ', 'Read saloon information', 'SALOON', 'READ'),
('SALOON_WRITE', 'Create and update saloons', 'SALOON', 'WRITE'),
('SALOON_DELETE', 'Delete saloons', 'SALOON', 'DELETE'),

-- Appointment management permissions
('APPOINTMENT_READ', 'Read appointment information', 'APPOINTMENT', 'READ'),
('APPOINTMENT_WRITE', 'Create and update appointments', 'APPOINTMENT', 'WRITE'),
('APPOINTMENT_DELETE', 'Delete appointments', 'APPOINTMENT', 'DELETE'),

-- Booking management permissions
('BOOKING_READ', 'Read booking information', 'BOOKING', 'READ'),
('BOOKING_WRITE', 'Create and update bookings', 'BOOKING', 'WRITE'),
('BOOKING_DELETE', 'Delete bookings', 'BOOKING', 'DELETE');

-- Assign permissions to roles
-- Admin gets all permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'ADMIN';

-- User gets basic read permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'USER' 
AND p.name IN ('USER_READ', 'SALOON_READ', 'APPOINTMENT_READ', 'BOOKING_READ');

-- Saloon Owner gets saloon and appointment management permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'SALOON_OWNER' 
AND p.name IN ('SALOON_READ', 'SALOON_WRITE', 'APPOINTMENT_READ', 'APPOINTMENT_WRITE', 'BOOKING_READ', 'BOOKING_WRITE');

-- Employee gets limited permissions
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p 
WHERE r.name = 'EMPLOYEE' 
AND p.name IN ('SALOON_READ', 'APPOINTMENT_READ', 'APPOINTMENT_WRITE', 'BOOKING_READ');

-- Create default admin user (password: admin123)
-- Note: This should be changed in production
INSERT IGNORE INTO users (username, email, password, first_name, last_name, is_active, is_email_verified)
VALUES ('admin', 'admin@saloon.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9P2.nRs.b7nAjS6', 'System', 'Administrator', TRUE, TRUE);

-- Assign admin role to default admin user
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id 
FROM users u, roles r 
WHERE u.username = 'admin' AND r.name = 'ADMIN';

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);

COMMIT;
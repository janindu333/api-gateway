#!/usr/bin/env pwsh
# Quick start script for Redis on Windows
Write-Host "Starting Redis container..." -ForegroundColor Green

# Check if Redis container already exists
$existing = docker ps -a --filter "name=redis" --format "{{.Names}}"
if ($existing -eq "redis") {
    $running = docker ps --filter "name=redis" --filter "status=running" --format "{{.Names}}"
    if ($running -eq "redis") {
        Write-Host "Redis is already running!" -ForegroundColor Yellow
        exit 0
    } else {
        Write-Host "Starting existing Redis container..." -ForegroundColor Yellow
        docker start redis
        exit 0
    }
}

# Start Redis container
docker run -d --name redis -p 6379:6379 redis:7-alpine

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Redis started successfully on port 6379" -ForegroundColor Green
    Write-Host "You can now run your API Gateway application." -ForegroundColor Green
} else {
    Write-Host "✗ Failed to start Redis" -ForegroundColor Red
}


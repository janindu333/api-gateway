# =============================================================================
# Development Setup Script for Windows PowerShell
# =============================================================================

Write-Host "🚀 Setting up Saloon Microservices Development Environment" -ForegroundColor Green

# Check if Docker Desktop is running
Write-Host "📋 Checking Docker Desktop..." -ForegroundColor Yellow
try {
    docker version | Out-Null
    Write-Host "✅ Docker Desktop is running" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker Desktop is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Check if Kubernetes is enabled
Write-Host "📋 Checking Kubernetes..." -ForegroundColor Yellow
try {
    kubectl version --client | Out-Null
    Write-Host "✅ Kubernetes is available" -ForegroundColor Green
} catch {
    Write-Host "❌ Kubernetes is not available. Please enable Kubernetes in Docker Desktop." -ForegroundColor Red
    exit 1
}

# Create necessary directories
Write-Host "📁 Creating project directories..." -ForegroundColor Yellow
$directories = @("logs", "data", "monitoring", "scripts")
foreach ($dir in $directories) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir
        Write-Host "✅ Created directory: $dir" -ForegroundColor Green
    }
}

# Generate JWT Secret if not exists
Write-Host "🔐 Checking JWT Secret..." -ForegroundColor Yellow
if (!(Test-Path ".env")) {
    Write-Host "⚠️  .env file not found. Please create it from .env.example" -ForegroundColor Yellow
} else {
    $envContent = Get-Content ".env" -Raw
    if ($envContent -match "your-super-secret-jwt-key-for-development-only") {
        Write-Host "⚠️  Please update JWT_SECRET in .env file with a secure secret" -ForegroundColor Yellow
        Write-Host "💡 Generate one using: openssl rand -base64 32" -ForegroundColor Cyan
    }
}

# Build Docker images
Write-Host "🔨 Building Docker images..." -ForegroundColor Yellow
$services = @("discovery-server", "identity-service", "api-gateway", "appointment-service", "saloon-service", "booking-service", "notification-service")

foreach ($service in $services) {
    if (Test-Path "$service/Dockerfile") {
        Write-Host "Building $service..." -ForegroundColor Cyan
        docker build -t "saloon/$service:latest" "$service/"
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Built $service successfully" -ForegroundColor Green
        } else {
            Write-Host "❌ Failed to build $service" -ForegroundColor Red
        }
    } else {
        Write-Host "⚠️  Dockerfile not found for $service" -ForegroundColor Yellow
    }
}

# Setup Kubernetes namespace and secrets
Write-Host "☸️  Setting up Kubernetes resources..." -ForegroundColor Yellow
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml

Write-Host "✅ Development environment setup completed!" -ForegroundColor Green
Write-Host ""
Write-Host "🚀 Next steps:" -ForegroundColor Cyan
Write-Host "1. Start services with Docker Compose: docker-compose up -d" -ForegroundColor White
Write-Host "2. Or deploy to Kubernetes: kubectl apply -f k8s/" -ForegroundColor White
Write-Host "3. Access services:" -ForegroundColor White
Write-Host "   - API Gateway: http://localhost:8080" -ForegroundColor White
Write-Host "   - Eureka Dashboard: http://localhost:8761" -ForegroundColor White
Write-Host "   - Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor White
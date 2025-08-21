# 🚀 Localhost Development Guide for Microservices
## Docker Desktop + Kubernetes on Windows

### 🏗️ Current Architecture Analysis

Your setup includes:
- **API Gateway** (Spring Cloud Gateway) - Port 8080
- **Eureka Discovery Server** - Port 8761  
- **Identity Service** (Authentication/Authorization)
- **Other Services** (Appointment, Saloon, Booking, Notification)
- **MySQL Database**

## 🔧 Critical Issues Fixed

### 1. **Security Improvements Made**

✅ **Fixed Authentication Filter:**
- Proper HTTP status codes (401 for unauthorized)
- Better error handling and logging
- User context propagation to downstream services
- Request ID tracking for debugging

✅ **Improved Route Validator:**
- Explicit security mapping instead of implicit
- Support for different security levels (PUBLIC, AUTHENTICATED, ROLE_BASED, ADMIN_ONLY)
- Configurable Swagger endpoint security
- Better pattern matching for wildcards

✅ **JWT Security:**
- All secrets externalized to environment variables
- Proper token validation and claims extraction
- Role and permission-based access control

### 2. **Configuration Improvements**

✅ **Environment-based Configuration:**
- Separate configs for Docker and Kubernetes
- Proper health checks and monitoring
- Structured JSON logging for log aggregation
- Performance optimizations

## 📁 Improved Project Structure

```
saloon-microservices/
├── api-gateway/
│   ├── src/main/java/com/baber/apigateway/
│   │   ├── filter/
│   │   │   ├── AuthenticationFilter.java ✅ IMPROVED
│   │   │   └── RouteValidator.java ✅ IMPROVED
│   │   ├── dto/
│   │   │   └── ErrorResponse.java ✅ NEW
│   │   └── service/
│   │       └── JwtService.java ✅ NEW
│   └── src/main/resources/
│       ├── application.properties
│       └── application-docker.properties ✅ NEW
├── identity-service/
├── discovery-server/
├── other-services/
├── docker-compose.yml ✅ IMPROVED
├── .env ✅ NEW
├── k8s/ ✅ NEW
│   ├── namespace.yaml
│   ├── secrets.yaml
│   └── mysql-deployment.yaml
└── scripts/ ✅ NEW
    ├── setup-dev.ps1
    └── init-db.sql
```

## 🚀 Quick Start Guide

### Option 1: Docker Compose (Recommended for Development)

1. **Setup Environment:**
   ```powershell
   # Run the setup script
   .\scripts\setup-dev.ps1
   
   # Or manually:
   # 1. Copy .env file and update secrets
   # 2. Build Docker images
   # 3. Start services
   ```

2. **Start All Services:**
   ```bash
   docker-compose up -d
   ```

3. **Check Service Health:**
   ```bash
   docker-compose ps
   curl http://localhost:8080/actuator/health
   curl http://localhost:8761  # Eureka Dashboard
   ```

### Option 2: Kubernetes (For Production-like Testing)

1. **Deploy to Kubernetes:**
   ```bash
   kubectl apply -f k8s/namespace.yaml
   kubectl apply -f k8s/secrets.yaml
   kubectl apply -f k8s/mysql-deployment.yaml
   ```

2. **Port Forward Services:**
   ```bash
   kubectl port-forward -n saloon-system svc/api-gateway 8080:8080
   kubectl port-forward -n saloon-system svc/discovery-server 8761:8761
   ```

## 🔐 Security Best Practices Implemented

### 1. **JWT Token Management**
- Secure token validation with proper error handling
- User context propagation to downstream services
- Role and permission-based access control
- Token expiration and refresh handling

### 2. **Route Security**
- Explicit endpoint security mapping
- Different security levels (PUBLIC, AUTHENTICATED, ROLE_BASED, ADMIN_ONLY)
- Proper CORS configuration
- Request/Response logging for audit

### 3. **Database Security**
- Dedicated database user with limited privileges
- Connection pooling with leak detection
- Prepared statements to prevent SQL injection
- Password encryption using BCrypt

## 📊 Monitoring & Observability

### Health Checks
- **API Gateway:** http://localhost:8080/actuator/health
- **Eureka:** http://localhost:8761/actuator/health
- **Identity Service:** http://localhost:8081/actuator/health

### Swagger Documentation
- **API Gateway:** http://localhost:8080/swagger-ui.html
- **Individual Services:** Available through gateway routing

### Metrics & Monitoring
- Prometheus metrics: http://localhost:8080/actuator/prometheus
- Grafana dashboard: http://localhost:3000 (admin/admin)
- Zipkin tracing: http://localhost:9411

## 🛠️ Development Workflow

### 1. **Making Changes**
```bash
# 1. Make code changes
# 2. Rebuild specific service
docker-compose build identity-service

# 3. Restart specific service
docker-compose up -d identity-service

# 4. Check logs
docker-compose logs -f identity-service
```

### 2. **Testing**
```bash
# Test authentication
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Test secured endpoint
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/auth/profile
```

### 3. **Database Access**
```bash
# Connect to MySQL
docker exec -it saloon-mysql mysql -u saloon_user -p saloon_service
```

## 🔧 Troubleshooting

### Common Issues:

1. **Services not registering with Eureka:**
   - Check network connectivity
   - Verify Eureka credentials
   - Check service health endpoints

2. **JWT Authentication failing:**
   - Verify JWT_SECRET is consistent across services
   - Check token expiration
   - Validate token format

3. **Database connection issues:**
   - Ensure MySQL is healthy
   - Check database credentials
   - Verify network connectivity

### Debug Commands:
```bash
# Check service logs
docker-compose logs -f <service-name>

# Check network connectivity
docker exec -it <container> ping <target-service>

# Check environment variables
docker exec -it <container> env | grep -E "(JWT|DB|EUREKA)"
```

## 📈 Performance Optimization

### 1. **Connection Pooling**
- HikariCP with optimized settings
- Connection leak detection
- Proper timeout configurations

### 2. **Caching**
- Redis integration for session management
- JWT token caching
- Database query result caching

### 3. **Load Balancing**
- Eureka-based service discovery
- Client-side load balancing with Ribbon
- Circuit breaker patterns with Resilience4j

## 🚀 Next Steps

1. **Implement remaining services** using the same patterns
2. **Add comprehensive testing** (unit, integration, contract)
3. **Set up CI/CD pipeline** with security scanning
4. **Implement distributed tracing** with Sleuth and Zipkin
5. **Add API rate limiting** and throttling
6. **Implement audit logging** for compliance
7. **Set up monitoring alerts** with Prometheus and Grafana

## 📚 Additional Resources

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [Spring Security JWT](https://spring.io/guides/tutorials/spring-boot-oauth2/)
- [Docker Compose Best Practices](https://docs.docker.com/compose/production/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
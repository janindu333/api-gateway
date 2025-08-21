# Spring Boot Configuration Guide for Docker & Kubernetes

## 🚨 Critical Security Issues Found

Your current configurations have several security vulnerabilities that need immediate attention:

### 1. **Hardcoded Secrets** ❌
```properties
# NEVER do this in production
jwt.secret=U2dWa1lwMzczNjc5NzkyRjQyRjQ1Mjg0ODJCNGRiNjI1MTY1NTQ2ODU3NmQ1YTcxNDc0Nw==
spring.datasource.password=root
eureka.url=http://eureka:password@localhost:8761/eureka
```

### 2. **Insecure Database Settings** ❌
```properties
# Problematic settings
spring.jpa.hibernate.ddl-auto=update  # Should be 'validate' in production
spring.jpa.show-sql=true              # Should be false in production
spring.datasource.username=root       # Use dedicated service account
```

## ✅ Industry Standard Solutions

### 1. **Environment-Based Configuration**

Use the improved configuration files I've created:
- `application-docker.properties` - For Docker environments
- `application-k8s-improved.properties` - For Kubernetes environments

### 2. **Secrets Management**

#### For Docker:
```bash
# Use Docker secrets or environment variables
docker run -e JWT_SECRET="$(openssl rand -base64 32)" your-app
```

#### For Kubernetes:
```yaml
# Create secrets
apiVersion: v1
kind: Secret
metadata:
  name: identity-service-secrets
type: Opaque
data:
  jwt-secret: <base64-encoded-secret>
  db-password: <base64-encoded-password>
  eureka-password: <base64-encoded-password>
```

### 3. **Database Configuration Best Practices**

```properties
# Production settings
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.leak-detection-threshold=60000
```

### 4. **Logging Best Practices**

#### For Kubernetes (JSON format for log aggregation):
```properties
logging.pattern.console={"timestamp":"%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX}","level":"%level","service":"${spring.application.name}","pod":"${HOSTNAME}","message":"%msg%n"}
```

### 5. **Health Checks & Monitoring**

```properties
# Kubernetes-optimized health checks
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true
management.endpoint.health.group.liveness.include=livenessState,diskSpace
management.endpoint.health.group.readiness.include=readinessState,db,eureka
```

## 🐳 Docker Configuration

### Dockerfile Best Practices:
```dockerfile
FROM openjdk:17-jre-slim
COPY target/identity-service.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Docker Compose:
```yaml
version: '3.8'
services:
  identity-service:
    image: identity-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JWT_SECRET=${JWT_SECRET}
      - DB_PASSWORD=${DB_PASSWORD}
    depends_on:
      - mysql
      - discovery-server
```

## ☸️ Kubernetes Configuration

### Deployment with Proper Resource Limits:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: identity-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: identity-service
        image: identity-service:latest
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: identity-service-secrets
              key: jwt-secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
```

## 📊 Monitoring & Observability

### Prometheus Metrics:
```properties
management.metrics.export.prometheus.enabled=true
management.metrics.tags.application=${spring.application.name}
management.metrics.tags.environment=kubernetes
```

### Distributed Tracing:
```properties
spring.sleuth.enabled=true
spring.zipkin.enabled=true
management.tracing.sampling.probability=0.1  # 10% sampling for production
```

## 🔧 Performance Tuning

### Connection Pool Optimization:
```properties
# For high-traffic applications
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### JVM Tuning for Containers:
```bash
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
```

## 🚀 Migration Steps

1. **Immediate Actions:**
   - Replace hardcoded secrets with environment variables
   - Set `spring.jpa.hibernate.ddl-auto=validate` in production
   - Disable SQL logging in production

2. **Short Term:**
   - Implement proper secrets management
   - Add structured JSON logging
   - Configure proper health checks

3. **Long Term:**
   - Implement distributed tracing
   - Add comprehensive monitoring
   - Set up proper CI/CD with security scanning

## 🔒 Security Checklist

- [ ] Remove all hardcoded secrets
- [ ] Use Kubernetes secrets or Docker secrets
- [ ] Enable HTTPS/TLS
- [ ] Configure proper CORS settings
- [ ] Implement rate limiting
- [ ] Add security headers
- [ ] Use non-root database users
- [ ] Enable audit logging
- [ ] Implement circuit breakers
- [ ] Set up proper network policies

## 📝 Environment Variables Template

Use the `env-template.properties` file as a reference for all configurable environment variables.

## 🔍 Validation

After implementing these changes, validate your configuration:

```bash
# Check for hardcoded secrets
grep -r "password\|secret" src/main/resources/

# Validate Kubernetes deployment
kubectl apply --dry-run=client -f deployment.yaml

# Test health endpoints
curl http://localhost:8080/actuator/health
```

Remember: **Security first, performance second, convenience last!**
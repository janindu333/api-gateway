# Security Best Practices for Saloon Booking System

## 🔒 Critical Security Recommendations

### 1. **JWT Secret Management**
```bash
# Generate a strong JWT secret (minimum 256 bits)
openssl rand -base64 64
```

**Current Issue**: Your JWT secret is too simple and predictable.
**Fix**: Use the generated secret in your environment variables.

### 2. **Database Credentials**
**Current Issue**: Hardcoded credentials in properties files.
**Fix**: 
- Use environment variables for all database credentials
- Use Kubernetes secrets for production
- Consider using a secrets management service (HashiCorp Vault, AWS Secrets Manager)

### 3. **Environment-Specific Configurations**

#### Development Environment
```properties
# Use .env file (never commit to git)
JWT_SECRET=your_generated_secret_here
DB_PASSWORD=dev_password
EUREKA_PASSWORD=dev_eureka_password
```

#### Production Environment
```yaml
# Use Kubernetes secrets
apiVersion: v1
kind: Secret
metadata:
  name: production-secrets
data:
  jwt-secret: <base64-encoded-secret>
  db-password: <base64-encoded-password>
```

### 4. **Network Security**

#### Docker Compose
```yaml
# Use internal networks only
networks:
  - internal-network

# Don't expose database ports externally
mysql:
  ports:
    - "127.0.0.1:3306:3306"  # Only localhost access
```

#### Kubernetes
```yaml
# Use Network Policies
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: database-network-policy
spec:
  podSelector:
    matchLabels:
      app: mysql
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: saloon-service
    ports:
    - protocol: TCP
      port: 3306
```

### 5. **API Security**

#### Rate Limiting
```properties
# Enable rate limiting in production
app.features.rate-limiting=true
spring.cloud.gateway.default-filters[0]=RequestRateLimiter=20, 1, 1s
```

#### CORS Configuration
```properties
# Restrict CORS in production
CORS_ALLOWED_ORIGINS=https://yourdomain.com
CORS_ALLOW_CREDENTIALS=false
```

### 6. **Logging and Monitoring**

#### Sensitive Data Masking
```properties
# Mask sensitive data in logs
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=WARN
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

#### Audit Logging
```properties
# Enable audit logging
app.features.audit-logging=true
```

### 7. **Health Checks and Monitoring**

#### Kubernetes Health Checks
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

### 8. **Container Security**

#### Dockerfile Best Practices
```dockerfile
# Use non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup
USER appuser

# Don't run as root
USER 1001:1001
```

#### Image Security
```bash
# Scan images for vulnerabilities
docker scan your-image:tag

# Use specific versions, not latest
FROM openjdk:17-jre-slim
```

### 9. **Secrets Management**

#### For Development
```bash
# Create .env file (add to .gitignore)
cp env-template.properties .env
# Edit .env with your secrets
```

#### For Production
```yaml
# Use external secrets management
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: production-secrets
spec:
  secretStoreRef:
    name: vault-backend
    kind: SecretStore
  target:
    name: production-secrets
  data:
  - secretKey: jwt-secret
    remoteRef:
      key: saloon/jwt-secret
```

### 10. **SSL/TLS Configuration**

#### Database SSL
```properties
# Enable SSL for database connections
spring.datasource.url=jdbc:mysql://localhost:3306/db?useSSL=true&requireSSL=true&verifyServerCertificate=true
```

#### API Gateway SSL
```yaml
# Use ingress with TLS
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - yourdomain.com
    secretName: yourdomain-tls
```

## 🚨 Immediate Actions Required

1. **Generate new JWT secret**:
   ```bash
   openssl rand -base64 64
   ```

2. **Update environment variables**:
   - Copy `env-template.properties` to `.env`
   - Update all secrets with secure values

3. **Remove hardcoded credentials**:
   - Update all properties files to use environment variables
   - Remove any hardcoded passwords/secrets

4. **Enable security features**:
   - Enable rate limiting
   - Configure proper CORS
   - Enable audit logging

5. **Update Kubernetes secrets**:
   - Generate new base64-encoded secrets
   - Update `k8s/secrets.yaml`

## 📋 Security Checklist

- [ ] JWT secret is at least 256 bits
- [ ] Database credentials are not hardcoded
- [ ] All secrets use environment variables
- [ ] Rate limiting is enabled
- [ ] CORS is properly configured
- [ ] Health checks are implemented
- [ ] Audit logging is enabled
- [ ] Container runs as non-root user
- [ ] Images are scanned for vulnerabilities
- [ ] Network policies are configured
- [ ] SSL/TLS is enabled for external communication
- [ ] Secrets are managed externally in production

## 🔧 Tools and Resources

- **Secret Generation**: `openssl rand -base64 64`
- **Image Scanning**: `docker scan`, Trivy, Snyk
- **Network Policies**: Calico, Cilium
- **Secrets Management**: HashiCorp Vault, AWS Secrets Manager, Azure Key Vault
- **SSL Certificates**: Let's Encrypt, cert-manager
- **Security Scanning**: OWASP ZAP, SonarQube 
# 🔧 Swagger UI Troubleshooting Guide

## 🚨 Current Issue: "Failed to load remote configuration"

This error occurs when Swagger UI tries to load API documentation from services that aren't running or properly configured.

## ✅ **Quick Fix Steps**

### Step 1: Check What's Actually Running
```bash
# Check if your services are running
curl http://localhost:8080/actuator/health
curl http://localhost:8761  # Eureka Dashboard

# Check if API Gateway is responding
curl http://localhost:8080/gateway/info
```

### Step 2: Access Swagger UI Correctly
Instead of: `http://localhost:8080/webjars/swagger-ui/index.html`
Use: `http://localhost:8080/swagger-ui.html`

### Step 3: Test API Gateway Documentation
```bash
# Test if API Gateway's own documentation is working
curl http://localhost:8080/v3/api-docs
```

### Step 4: Check Service Registration
Visit Eureka Dashboard: `http://localhost:8761`
- Verify which services are actually registered
- Only include registered services in Swagger configuration

## 🔧 **Configuration Fixes Applied**

### 1. **Updated RouteValidator**
✅ Added all Swagger-related endpoints to public endpoints:
- `/v3/api-docs`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/webjars/**`
- `/swagger-resources/**`

### 2. **Simplified Swagger Configuration**
✅ Started with only API Gateway documentation:
```properties
# Only API Gateway docs initially
springdoc.swagger-ui.urls[0].name=API Gateway
springdoc.swagger-ui.urls[0].url=/v3/api-docs
```

### 3. **Added OpenAPI Configuration**
✅ Created `OpenApiConfig.java` to provide proper API Gateway documentation

### 4. **Added Gateway Controller**
✅ Created `GatewayController.java` with endpoints for:
- `/gateway/info` - Gateway information
- `/gateway/health` - Health check
- `/gateway/routes` - Available routes

## 🚀 **Testing Your Fix**

### Test 1: Basic Gateway Endpoints
```bash
# Test gateway info
curl http://localhost:8080/gateway/info

# Test gateway health
curl http://localhost:8080/gateway/health

# Test available routes
curl http://localhost:8080/gateway/routes
```

### Test 2: Swagger Documentation
```bash
# Test API Gateway's OpenAPI spec
curl http://localhost:8080/v3/api-docs

# Access Swagger UI
# Open in browser: http://localhost:8080/swagger-ui.html
```

### Test 3: Service-Specific Documentation
Only test these if the services are actually running:
```bash
# Test identity service docs (if running)
curl http://localhost:8080/identity-service/v3/api-docs

# Test other service docs (if running)
curl http://localhost:8080/appointment-service/v3/api-docs
```

## 🔍 **Common Issues & Solutions**

### Issue 1: "Failed to load remote configuration"
**Cause:** Services in Swagger config aren't running
**Solution:** 
1. Start only the services you need
2. Update `application.properties` to include only running services:
```properties
# Comment out services that aren't running
# springdoc.swagger-ui.urls[1].name=Identity Service
# springdoc.swagger-ui.urls[1].url=/identity-service/v3/api-docs
```

### Issue 2: Swagger UI shows empty page
**Cause:** No API documentation available
**Solution:** 
1. Ensure API Gateway's own docs are working: `curl http://localhost:8080/v3/api-docs`
2. Check if OpenAPI dependencies are in your `pom.xml`

### Issue 3: 404 errors for service documentation
**Cause:** Services aren't registered with Eureka or aren't running
**Solution:**
1. Check Eureka dashboard: `http://localhost:8761`
2. Start the missing services
3. Wait for service registration (30-60 seconds)

## 📝 **Step-by-Step Service Addition**

### Start with API Gateway Only
1. Ensure API Gateway is running
2. Test: `http://localhost:8080/swagger-ui.html`
3. Should show only "API Gateway" in the dropdown

### Add Identity Service
1. Start Identity Service
2. Wait for Eureka registration
3. Uncomment in `application.properties`:
```properties
springdoc.swagger-ui.urls[1].name=Identity Service
springdoc.swagger-ui.urls[1].url=/identity-service/v3/api-docs
```
4. Restart API Gateway
5. Test: Should now show both services in Swagger UI

### Add Other Services
Repeat the same process for each service:
- Start the service
- Wait for registration
- Add to Swagger configuration
- Restart API Gateway
- Test

## 🎯 **Expected Results**

After applying these fixes:

1. **Swagger UI loads successfully** at `http://localhost:8080/swagger-ui.html`
2. **API Gateway documentation** is visible and functional
3. **Service dropdown** shows only running services
4. **No "Failed to load remote configuration" errors**

## 🚨 **If Issues Persist**

### Check Logs
```bash
# Check API Gateway logs for errors
docker logs api-gateway  # if using Docker
# or check your IDE console

# Look for errors related to:
# - Swagger configuration
# - Service discovery
# - Route resolution
```

### Verify Dependencies
Ensure your `pom.xml` includes:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>2.0.2</version>
</dependency>
```

### Reset Configuration
If all else fails, temporarily disable all service documentation:
```properties
# Keep only API Gateway
springdoc.swagger-ui.urls[0].name=API Gateway
springdoc.swagger-ui.urls[0].url=/v3/api-docs

# Comment out all other services
# springdoc.swagger-ui.urls[1].name=...
```

This should give you a working Swagger UI with just the API Gateway documentation, then you can add services one by one as they become available.
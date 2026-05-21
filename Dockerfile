# Build stage: compile and package inside Docker
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests clean package

# Runtime stage: lightweight JRE image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Kubernetes Deployment sets SPRING_PROFILES_ACTIVE=k8s
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]

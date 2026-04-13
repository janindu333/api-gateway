FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Kubernetes Deployment sets SPRING_PROFILES_ACTIVE=k8s
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]

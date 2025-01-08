# Use an official Java runtime as a parent image
FROM openjdk:21-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the application JAR file into the container
COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the API Gateway will run on
EXPOSE 8081

# Command to run the API Gateway
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]

# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="ShopSphere" \
      description="API Gateway Service for ShopSphere Microservices"

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/api-gateway-1.0.0-SNAPSHOT.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Set JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Default environment variables
ENV REDIS_HOST=redis \
    REDIS_PORT=6379 \
    AUTH_SERVICE_URL=http://auth-service:8081 \
    EUREKA_URI=http://discovery-service:8761/eureka \
    SPRING_PROFILES_ACTIVE=prod

# Run application
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar

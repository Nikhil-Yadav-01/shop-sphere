FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY checkout-service/target/checkout-service.jar app.jar
EXPOSE 8086
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 CMD curl -f http://localhost:8086/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
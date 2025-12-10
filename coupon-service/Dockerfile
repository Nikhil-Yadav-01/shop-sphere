FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY coupon-service/target/coupon-service.jar app.jar
EXPOSE 8088
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 CMD curl -f http://localhost:8088/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1001 appuser && adduser -u 1001 -G appuser -s /sbin/nologin -D appuser && apk add --no-cache curl
WORKDIR /app
ARG JAR_FILE=media-service/target/media-service.jar
COPY ${JAR_FILE} app.jar
RUN mkdir -p /app/uploads && chmod 777 /app/uploads
USER appuser
EXPOSE 8091
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 CMD curl -f http://localhost:8091/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

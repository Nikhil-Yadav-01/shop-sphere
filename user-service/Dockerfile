FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1001 appuser && adduser -u 1001 -G appuser -s /sbin/nologin -D appuser && apk add --no-cache curl
WORKDIR /app
USER appuser
ARG JAR_FILE=user-service/target/user-service-*.jar
ARG MACHINE_IP=127.0.0.1
COPY ${JAR_FILE} app.jar
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 CMD curl -f http://${MACHINE_IP}:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

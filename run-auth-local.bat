@echo off
echo Starting Auth Service locally without Docker...

REM Set environment variables for local testing
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=shopsphere_auth
set DB_USERNAME=postgres
set DB_PASSWORD=password
set REDIS_HOST=localhost
set REDIS_PORT=6379
set KAFKA_BOOTSTRAP_SERVERS=localhost:9092
set JWT_SECRET=mySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong
set MAIL_HOST=smtp.gmail.com
set MAIL_PORT=587

echo Environment configured for local testing
echo Starting auth service on port 8081...

cd auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
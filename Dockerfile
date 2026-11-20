# ---- Build Stage ----
FROM eclipse-temurin:21-jdk-alpine AS builder

# Install minimal required tools
RUN apk add --no-cache bash unzip

# Set workdir
WORKDIR /app

# Copy everything
COPY . .

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the application (skip tests for speed; remove -x test to run them)
RUN ./gradlew clean bootJar -x test

# ---- Runtime Stage ----
# debian13: java21-debian12 is frozen at the affected 21.0.10; debian13 ships the
# Java4Shell (CVE-2026-114938) fix, 21.0.13.1.
FROM gcr.io/distroless/java21-debian13:nonroot

WORKDIR /app

# Copy the built jar
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
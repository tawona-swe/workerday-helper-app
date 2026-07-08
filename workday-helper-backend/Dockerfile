# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first (leverages Docker cache)
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Fix mvnw: ensure LF line endings and execute permission (Windows Git can corrupt these)
RUN apk add --no-cache dos2unix && \
    dos2unix mvnw && \
    chmod +x mvnw

# Download dependencies (separate from source for better caching)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src src
RUN ./mvnw package -DskipTests -B

# ---- Stage 2: Run ----
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create data directory for H2 database
RUN mkdir -p /app/data

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Render provides the port via $PORT env var; Spring Boot uses SERVER_PORT
# Map PORT -> server.port as a JVM argument
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]

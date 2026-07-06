# ---- Stage 1: Build ----
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first (leverages Docker cache)
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies (separate from source for better caching)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src src
RUN ./mvnw package -DskipTests -B

# ---- Stage 2: Run ----
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create data directory for H2 database
RUN mkdir -p /app/data

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

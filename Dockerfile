# ============================================================
# NOTIVIO - Multi-stage Dockerfile
# Stage 1: Build with Maven
# Stage 2: Run with JRE 21 slim
# ============================================================

# ── Stage 1: Build ──────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Cache dependencies layer (copy pom.xml first)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build the application
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S notivio && adduser -S notivio -G notivio

# Install curl for health checks
RUN apk add --no-cache curl

# Copy built jar
COPY --from=builder /app/target/notivio-backend-*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R notivio:notivio /app

USER notivio

EXPOSE 8080

# JVM settings optimized for containers
ENV JAVA_OPTS="-Xms256m -Xmx512m \
    -XX:+UseG1GC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

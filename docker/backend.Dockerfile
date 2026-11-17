# ==========================================
# AgentForge Backend Dockerfile
# Multi-stage build for Spring Boot 3.x on Java 21
# ==========================================

# Stage 1: Build JAR
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace/app

# Copy pom.xml and dependency definitions first for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and pre-compiled static assets
COPY src src
RUN mvn clean package -DskipTests -B

# Stage 2: Production Runtime
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Add curl for healthchecks and bash for script runners
RUN apk add --no-cache curl bash git

# Create non-root application user for sandbox security
RUN addgroup -S agentforge && adduser -S agentforge -G agentforge
USER agentforge:agentforge

# Copy built artifact from builder stage
COPY --from=builder /workspace/app/target/*.jar app.jar

ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/ai-config || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

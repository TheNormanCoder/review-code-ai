# Multi-stage build for optimal image size
FROM maven:3.9.8-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Create a standalone POM for ai-review-core
COPY ai-review-core/pom.xml ./original-pom.xml

# Copy standalone POM
COPY ai-review-core/pom-docker.xml ./pom.xml

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code and configuration files
COPY ai-review-core/src ./src
COPY ai-review-core/custom-pmd-rules.xml ./
COPY ai-review-core/checkstyle.xml ./
COPY ai-review-core/spotbugs-exclude.xml ./

# Build the application
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy configuration files
COPY --from=build /app/custom-pmd-rules.xml ./
COPY --from=build /app/checkstyle.xml ./

# Change ownership of the app directory
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set JVM options for containerized environment with Java 21 optimizations
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseZGC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat --enable-preview"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
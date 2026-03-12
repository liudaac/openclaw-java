# OpenClaw Server Dockerfile
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom
COPY ../pom.xml .
COPY ../.mvn ./.mvn
COPY ../mvnw .

# Copy module pom
COPY pom.xml ./openclaw-server/

# Copy source code
COPY src ./openclaw-server/src

# Copy other modules (needed for dependencies)
COPY ../openclaw-plugin-sdk ./openclaw-plugin-sdk
COPY ../openclaw-gateway ./openclaw-gateway
COPY ../openclaw-security ./openclaw-security
COPY ../openclaw-agent ./openclaw-agent
COPY ../openclaw-memory ./openclaw-memory
COPY ../openclaw-secrets ./openclaw-secrets
COPY ../openclaw-tools ./openclaw-tools
COPY ../openclaw-channel-telegram ./openclaw-channel-telegram
COPY ../openclaw-channel-feishu ./openclaw-channel-feishu

# Build the application
RUN ./mvnw clean package -DskipTests -pl openclaw-server -am

# Create runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built artifact
COPY --from=0 /app/openclaw-server/target/openclaw-server-*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/gateway/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copy the maven wrapper and pom.xml first to cache dependencies
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
# Ensure maven wrapper has execute permissions
RUN chmod +x mvnw
# Download dependencies (this step will be cached if pom.xml doesn't change)
RUN ./mvnw --batch-mode dependency:go-offline

# Copy the source code and build the application
COPY src ./src
RUN ./mvnw --batch-mode clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Reports are filesystem-backed. Keep their root explicit and writable by the
# unprivileged runtime user; production must attach a named/bind volume here.
ENV APP_UPLOAD_DIR=/app/uploads
RUN useradd --system --uid 10001 --create-home appuser \
    && mkdir -p "$APP_UPLOAD_DIR" \
    && chown -R appuser:appuser "$APP_UPLOAD_DIR"

# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port 8080
EXPOSE 8080
VOLUME ["/app/uploads"]

USER 10001

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]

# ─── Stage 1: build the jar ──────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the POM first and pre-download dependencies. This layer is cached, so
# code-only changes rebuild in seconds instead of re-fetching all of Maven.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ─── Stage 2: runtime ────────────────────────────────────────
# JRE only - no Maven, no compiler, no source. Much smaller and less to attack.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Don't run as root.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /build/target/*.jar app.jar

# Railway injects $PORT; application.yml binds to it.
EXPOSE 8080

# Container-aware heap sizing so the JVM respects Railway's memory limit.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

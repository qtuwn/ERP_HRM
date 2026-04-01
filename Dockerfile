# syntax=docker/dockerfile:1
# Build: docker build -t erp-hrm:latest .
# Chạy:  docker run --rm -p 8080:8080 erp-hrm:latest

# --- Giai đoạn 1: Vite → src/main/resources/static ---
FROM node:22-bookworm-slim AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./frontend/
# Dùng npm install (không dùng ci) để tránh fail khi lock hơi lệch; ưu tiên vẫn là lock đồng bộ trên repo.
RUN cd frontend && npm install --no-audit --no-fund
COPY frontend ./frontend
RUN mkdir -p src/main/resources/static \
    && cd frontend && npm run build

# --- Giai đoạn 2: Maven package (JAR có sẵn bundle SPA) ---
FROM eclipse-temurin:21-jdk-jammy AS backend
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src ./src
COPY --from=frontend /app/src/main/resources/static ./src/main/resources/static
RUN chmod +x mvnw && ./mvnw -B -q package -DskipTests \
    && mv /app/target/erp-hrm-*.jar /app/application.jar

# --- Giai đoạn 3: JRE tối giản + curl (healthcheck docker-compose) ---
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=backend /app/application.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

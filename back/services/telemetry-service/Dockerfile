# ========================================================
# Dockerfile para telemetry-service
# ========================================================

# Estágio 1: Build da aplicação com Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src src

RUN mvn clean package -DskipTests

# ========================================================
# Estágio 2: Imagem final com JRE - USAR VERSÃO SEM ALPINE
FROM eclipse-temurin:17-jre-jammy  

WORKDIR /app

RUN mkdir -p /app/logs && \
    chmod 755 /app/logs && \
    groupadd -r spring && useradd -r -g spring spring && \
    chown -R spring:spring /app/logs

USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
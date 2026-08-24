FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/target/telemetry-service-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 9050

ENTRYPOINT ["java", "-jar", "app.jar"]
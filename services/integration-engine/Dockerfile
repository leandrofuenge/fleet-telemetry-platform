FROM docker.io/library/maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
COPY schemas ./schemas
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

FROM docker.io/library/eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app --shell /usr/sbin/nologin spring \
    && apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder --chown=spring:spring /workspace/target/integration-engine-0.0.1-SNAPSHOT.jar /app/app.jar

USER spring:spring

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 9060

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM maven:3.9.16-eclipse-temurin-21-noble AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine-3.23 AS runtime

RUN apk upgrade --no-cache \
    && addgroup -S -g 10001 appgroup \
    && adduser -S -D -H -u 10001 -G appgroup appuser

WORKDIR /app
COPY --from=build --chown=appuser:appgroup /workspace/target/geradornotafiscal-*.jar /app/app.jar

USER 10001:10001
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -q -O - http://localhost:8080/livez || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

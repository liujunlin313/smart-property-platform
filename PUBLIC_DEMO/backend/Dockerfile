# Root build context is required: backend/pom.xml packages ../database/migrations as resources.
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
COPY database/migrations database/migrations
COPY backend/src backend/src
RUN mvn --batch-mode --file backend/pom.xml --define skipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --create-home app \
    && mkdir -p /app/logs/run-unknown \
    && chown -R app:app /app

WORKDIR /app
COPY --from=builder --chown=app:app /workspace/backend/target/smart-property-backend-*.jar /app/app.jar

EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN mkdir -p /var/data/uploads \
    && useradd --system --uid 10001 --no-create-home appuser \
    && chown -R appuser:appuser /app /var/data
COPY --from=build --chown=appuser:appuser /app/target/hssc-backend-1.0.0.jar /app/app.jar

USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

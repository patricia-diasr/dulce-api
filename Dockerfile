FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN useradd --system --uid 10001 appuser

COPY --from=build --chown=10001:10001 /workspace/target/dulce-api-*.jar /app/app.jar

USER 10001

ENV SERVER_PORT=10000

EXPOSE 10000

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60.0", "-jar", "/app/app.jar"]

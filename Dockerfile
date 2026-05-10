# Stage 1 — Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -q

# Stage 2 — Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/server-error-log-manager-1.0-jar-with-dependencies.jar app.jar

# Render sets PORT dynamically — don't hardcode it here
# Our Main.java reads System.getenv("PORT") which Render injects
EXPOSE 10000
CMD ["java", "-jar", "app.jar"]

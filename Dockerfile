# Stage 1 — Build using Maven + Java 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -q

# Stage 2 — Run using just Java 17 (smaller final image)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/server-error-log-manager-1.0-jar-with-dependencies.jar app.jar
EXPOSE 10000
ENV PORT=10000
CMD ["java", "-jar", "app.jar"]

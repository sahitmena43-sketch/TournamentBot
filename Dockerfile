FROM maven:3.9-eclipse-temurin-17-alpine

WORKDIR /app

# Kopjo kodin
COPY pom.xml .
COPY src/ src/

# Build (pa dependency:go-offline)
RUN mvn clean package

# Kopjo JAR-in
COPY target/TournamentBot-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
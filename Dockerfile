# Përdor imazhin e saktë
FROM eclipse-temurin:17-jdk-alpine

# Vendos direktorinë e punës
WORKDIR /app

# Kopjo pom.xml dhe shkarko dependency-t
COPY pom.xml .
RUN apk add --no-cache maven
RUN mvn dependency:go-offline

# Kopjo kodin dhe buildo
COPY src/ src/
RUN mvn clean package

# Kopjo JAR-in final
COPY target/TournamentBot-1.0-SNAPSHOT.jar app.jar

# Porti për health check
EXPOSE 8080

# Starto bot-in
ENTRYPOINT ["java", "-jar", "app.jar"]
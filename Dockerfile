# Use Eclipse Temurin Java 21 with Maven wrapper
FROM eclipse-temurin:21-jdk AS build

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline

# Copy the full source
COPY src src

# Package the application
RUN ./mvnw clean package -DskipTests

# Use a lightweight JRE base image
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expose default Spring Boot port
EXPOSE 80

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]

# Use an official OpenJDK image
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/gemini-ethos-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]

# base image https://hub.docker.com/layers/library/openjdk/17-jdk-alpine/
# FROM openjdk:17-jdk-alpine
# https://hub.docker.com/_/amazoncorretto/
# FROM amazoncorretto:25-jdk
# https://hub.docker.com/_/eclipse-temurin
FROM eclipse-temurin:25-jdk-alpine

# Set environment variables ref: https://docs.docker.com/build/building/variables/#env-usage-example
ARG JAR_FILE
# ENV JAR_FILE=spring-petclinic-3.4.0-SNAPSHOT.jar

WORKDIR /app

COPY ${JAR_FILE} /app/spring-petclinic.jar

# Set the command to run the Spring Boot application
# java -jar target/spring-petclinic-3.2.0-SNAPSHOT.jar --server.port=7080 
# CMD java -jar ${JAR_FILE} 
CMD ["java", "-jar", "spring-petclinic.jar"]
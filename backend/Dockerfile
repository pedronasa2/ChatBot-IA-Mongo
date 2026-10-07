FROM maven:3.10.0-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:26.0.2.1_1-jre-noble
WORKDIR /app
COPY --from=build /app/target/painel-bot-0.1.0.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

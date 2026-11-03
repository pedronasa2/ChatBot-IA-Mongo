FROM maven:3.10.0-eclipse-temurin-25 AS build
WORKDIR /app
COPY . ./
RUN mvn -B -DskipTests package
FROM eclipse-temurin:25.0.5_8-jre-noble
WORKDIR /app
COPY --from=build /app/chatbot-boot/target/painel-bot-0.1.0.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

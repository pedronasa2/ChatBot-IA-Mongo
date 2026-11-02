FROM node:26.10.0-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/angular.json frontend/tsconfig.json ./
COPY frontend/src src
RUN npm run build
FROM maven:3.10.0-eclipse-temurin-26 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src src
COPY --from=frontend /frontend/dist/painel/browser/ src/main/resources/static/
RUN mvn -B -DskipTests package
FROM eclipse-temurin:26.0.2.1_1-jre-noble
WORKDIR /app
COPY --from=build /app/target/painel-bot-0.1.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=docker
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Образ в две стадии: сборка на JDK, запуск на JRE
# Каждый контейнер - отдельное приложение Spring Boot
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src
COPY . .
ARG MODULE
RUN chmod +x gradlew && ./gradlew :${MODULE}:bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ARG MODULE
COPY --from=build /src/${MODULE}/build/libs/${MODULE}.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

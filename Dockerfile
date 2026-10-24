# Multi-stage image: compile with a JDK, run with a JRE.
# Each container is still one independent Spring Boot service.
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

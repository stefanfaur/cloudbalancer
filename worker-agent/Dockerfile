FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache docker-cli
WORKDIR /app
COPY worker-agent/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

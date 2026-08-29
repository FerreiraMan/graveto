FROM eclipse-temurin:21

WORKDIR /app

ARG JAR_FILE=build/libs/graveto.jar
ARG HEALTH_CHECK_CLIENT=src/main/java/me/ferreira/graveto/common/infrastructure/http/HealthCheckClient.java

COPY ${JAR_FILE} graveto-app.jar
COPY ${HEALTH_CHECK_CLIENT} .

ENTRYPOINT ["java","-jar","graveto-app.jar"]

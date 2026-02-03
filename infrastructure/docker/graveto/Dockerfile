FROM eclipse-temurin:21

WORKDIR /app
ARG JAR_FILE=build/libs/graveto.jar
COPY ${JAR_FILE} graveto-app.jar

ENTRYPOINT ["java","-jar","graveto-app.jar"]

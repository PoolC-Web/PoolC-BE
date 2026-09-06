FROM openjdk:11-jdk-slim
COPY build/libs/app.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

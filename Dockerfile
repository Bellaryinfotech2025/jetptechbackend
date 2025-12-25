FROM eclipse-temurin:17-jdk-alpine

WORKDIR /application

COPY target/*.jar application.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/application/application.jar"]

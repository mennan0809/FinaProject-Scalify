FROM openjdk:25-ea-4-jdk-oraclelinux9

WORKDIR /app

COPY target/UserService.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "UserService.jar"]

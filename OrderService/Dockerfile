FROM openjdk:25-ea-4-jdk-oraclelinux9

WORKDIR /app

COPY target/OrderService.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "OrderService.jar"]

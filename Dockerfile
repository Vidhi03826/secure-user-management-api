FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/secure-user-management-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
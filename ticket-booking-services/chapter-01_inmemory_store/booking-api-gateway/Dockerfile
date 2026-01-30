FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY /build/libs/booking-api-gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 6500

ENTRYPOINT ["java", "-jar", "app.jar"]


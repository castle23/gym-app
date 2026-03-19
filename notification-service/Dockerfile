FROM maven:3.9.0-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml ./pom.xml
COPY common/ ./common/
COPY api-gateway/ ./api-gateway/
COPY auth-service/ ./auth-service/
COPY training-service/ ./training-service/
COPY tracking-service/ ./tracking-service/
COPY notification-service/ ./notification-service/
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /build/notification-service/target/notification-service-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]

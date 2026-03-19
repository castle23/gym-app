FROM maven:3.9.0-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml ../pom.xml
COPY auth-service/ ./
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /build/target/auth-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

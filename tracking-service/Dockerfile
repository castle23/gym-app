FROM maven:3.9.0-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml ../pom.xml
COPY tracking-service/ ./
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /build/target/tracking-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

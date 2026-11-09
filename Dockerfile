FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/vcognirule-1.0.0.jar app.jar
RUN mkdir -p database
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
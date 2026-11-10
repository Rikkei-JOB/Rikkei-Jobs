# Build context: server/ (see server/services/README.md).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -pl services/company-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/company-service/target/company-service-*.jar app.jar
USER 10001:10001
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


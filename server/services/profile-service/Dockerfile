# Build context: server/ (see server/services/README.md).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -pl services/profile-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/profile-service/target/profile-service-*.jar app.jar
RUN mkdir -p /app/data/profile-files && chown -R 10001:10001 /app/data
USER 10001:10001
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]


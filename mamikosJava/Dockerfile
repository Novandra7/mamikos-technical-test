# Build stage: keep the Maven cache in its own layer so editing application code
# does not force every dependency to be re-downloaded.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests -Dspotless.check.skip=true -Dcheckstyle.skip=true

# Runtime stage: JRE only, non-root user, nothing left over from the build.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/mamikos-kost-api.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

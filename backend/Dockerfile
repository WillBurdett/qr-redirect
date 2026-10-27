# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Copy pom first so dependency downloads are cached unless the pom changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 --no-create-home app
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50", "-jar", "app.jar"]
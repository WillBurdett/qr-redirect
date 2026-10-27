# Set to the java.version in backend/pom.xml (21, or 25 if you finished that upgrade)
ARG JAVA_VERSION=21

# 1. Build the React app
FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2. Build the jar, with the React build bundled in as static files
FROM maven:3.9-eclipse-temurin-${JAVA_VERSION} AS backend
WORKDIR /app
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /app/dist ./src/main/resources/static
RUN mvn -q -DskipTests package

# 3. Run
FROM eclipse-temurin:${JAVA_VERSION}-jre
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
RUN useradd --system --no-create-home app
USER app
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
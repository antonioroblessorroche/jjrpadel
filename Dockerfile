# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache de dependencias
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Copiamos el código y compilamos en prod (Vaadin)
COPY src ./src
# Si tienes carpeta frontend/ en Vaadin Flow:
COPY frontend ./frontend
# Build producción Vaadin + Spring Boot
RUN mvn -DskipTests clean package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# TZ (tu server está en España)
ENV TZ=Europe/Madrid \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SERVER_PORT=8080

# Certificados raíz (por si llamas a APIs HTTPS)
RUN apk add --no-cache curl ca-certificates tzdata

# Copiamos el jar final (ajusta el nombre si tu artifact es distinto)
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080

# Variables de entorno típicas de Spring Boot
# (mantén secretos fuera de la imagen; se pasan en docker run)

# Arranque
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar --server.port=${SERVER_PORT}"]

# services/notificationEntity-service/Dockerfile

# --- Estágio 1: Build (Compila o Java) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Copia todos os POMs
COPY pom.xml .
COPY libs/outbox-relay/pom.xml ./libs/outbox-relay/
COPY services/scheduler-service/pom.xml ./services/scheduler-service/
COPY services/notificationEntity-service/pom.xml ./services/notificationEntity-service/
COPY services/history-service/pom.xml ./services/history-service/

# 2. Baixa as dependências
RUN mvn verify --fail-never

# 3. Copia todo o código-fonte
COPY libs/ ./libs/
COPY services/ ./services/

# 4. Constrói o 'notificationEntity-service' E suas dependências
RUN mvn clean package -DskipTests -pl services/notificationEntity-service -am

# --- Estágio 2: Run (Executa o JAR) ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 1. Copia o JAR construído
COPY --from=build /app/services/notificationEntity-service/target/notificationEntity-service-*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
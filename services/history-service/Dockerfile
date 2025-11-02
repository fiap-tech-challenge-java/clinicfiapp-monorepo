# services/history-service/Dockerfile

# --- Estágio 1: Build (Compila o Java) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Copia todos os POMs
COPY pom.xml .
COPY libs/outbox-relay/pom.xml ./libs/outbox-relay/
COPY services/scheduler-service/pom.xml ./services/scheduler-service/
COPY services/notification-service/pom.xml ./services/notification-service/
COPY services/history-service/pom.xml ./services/history-service/

# 2. Baixa as dependências
RUN mvn verify --fail-never

# 3. Copia todo o código-fonte
COPY libs/ ./libs/
COPY services/ ./services/

# 4. Constrói o 'history-service' E suas dependências
RUN mvn clean package -DskipTests -pl services/history-service -am

# --- Estágio 2: Run (Executa o JAR) ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 1. Copia o JAR construído
COPY --from=build /app/services/history-service/target/history-service-*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
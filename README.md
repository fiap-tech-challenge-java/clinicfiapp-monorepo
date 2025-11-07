Absolutamente. Excelente ideia.

Ter um `README.md` claro é a ferramenta de onboarding mais importante para a equipe, especialmente agora que a fundação está estável. Isso garante que todos, incluindo os professores, entendam a arquitetura e saibam como executar o projeto em segundos.

Aqui está uma proposta de `README.md` para a raiz do projeto. Ele documenta o que fizemos, por que fizemos, e (o mais importante) como rodar.

-----

# ClinicFiapApp - Monorepo (Tech Challenge - Fase 3)

Este repositório contém o backend do projeto **ClinicFiapApp**, a solução para o Tech Challenge - Fase 3 do curso de Arquitetura e Desenvolvimento Java.

O objetivo é desenvolver um sistema hospitalar modular, seguro e assíncrono, focado em agendamento de consultas, gerenciamento de histórico de pacientes e envio de notificações.

## 🏛️ Arquitetura

Adotamos uma arquitetura de **Monorepo** gerenciada pelo Maven, contendo múltiplos microsserviços e bibliotecas compartilhadas. Toda a infraestrutura é orquestrada via **Docker Compose**.

### Módulos do Projeto

  * `pom.xml` (Raiz): POM Agregador que gerencia todos os módulos e dependências.
  * `infra/`: Contém scripts de inicialização da infraestrutura, como a criação automática dos bancos de dados.
  * `libs/outbox-relay/`: (Futuro) Biblioteca compartilhada para DTOs ou lógicas do padrão Outbox.
  * `services/scheduler-service/`: **(Core)** O "cérebro" da aplicação. Responsável por:
      * Gerenciamento de Usuários (Médicos, Pacientes, etc.).
      * Segurança (Spring Security).
      * Lógica de Agendamento de Consultas.
      * API principal (GraphQL).
      * Produção de eventos para o Kafka (usando o Padrão Outbox).
  * `services/notification-service/`: Microsserviço consumidor do Kafka, responsável por processar eventos e enviar notificações (ex: lembretes de consulta).
  * `services/history-service/`: Microsserviço consumidor do Kafka que atua como um "Read Model" (CQRS). Ele constrói uma projeção de dados otimizada para leitura do histórico de consultas.

## 🛠️ Stack Tecnológica

  * **Java 21**
  * **Spring Boot 3.5.7**
  * **Docker & Docker Compose**
  * **Banco de Dados:** PostgreSQL 17-alpine
  * **Mensageria:** Apache Kafka (Confluent-inc 7.6.1)
  * **API:** Spring for GraphQL
  * **Segurança:** Spring Security
  * **Migração de BD:** Flyway
  * **Build:** Maven

## 🚀 Como Executar o Projeto (One-Click Run)

Toda a infraestrutura (bancos de dados, Kafka) e os microsserviços são gerenciados pelo Docker Compose.

**Pré-requisitos:**

  * Docker e Docker Compose instalados.
  * Portas `5438`, `9092`, `2181`, `8080`, `8081`, `8082`, `8083` livres na sua máquina.

### 1\. Executando Tudo com Docker (Recomendado)

Com um único comando, toda a stack subirá, incluindo a criação automática dos bancos de dados e a compilação das aplicações.

No diretório raiz do projeto (onde está o `docker-compose.yml`), execute:

```bash
docker-compose up -d --build
```

**O que este comando faz:**

1.  **Inicia a Infra:** Sobe os contêineres `postgres`, `zookeeper`, `kafka` e `kafka-ui`.
2.  **Cria os Bancos:** O `postgres` executa o script em `infra/postgres/init/01-init-dbs.sql` e cria automaticamente os bancos `scheduler_db`, `notification_db` e `history_db`.
3.  **Constrói as Aplicações:** O Docker usa os `Dockerfiles` de cada serviço (ex: `services/scheduler-service/Dockerfile`) para compilar o código Java e gerar as imagens.
4.  **Inicia as Aplicações:** Inicia os contêineres `scheduler-service`, `notification-service` e `history-service`.
5.  **Resiliência:** Os serviços Java têm `restart: on-failure` para garantir que eles reiniciem caso tentem se conectar ao Postgres antes que este esteja pronto.

### 2\. Para Parar a Execução

```bash
docker-compose down
```

### 3\. Para Resetar (Apagar os Dados dos Bancos)

Se precisar apagar todos os volumes de dados (incluindo o `postgres_data`), use:

```bash
docker-compose down -v
```

### 4\. Desenvolvimento Local (Rodando pela IDE)

Se você preferir rodar um dos serviços (ex: `scheduler-service`) pela sua IDE para facilitar o debug:

1.  **Inicie apenas a infraestrutura:**

    ```bash
    docker-compose up -d postgres zookeeper kafka kafka-ui
    ```

2.  **Aguarde** a infraestrutura estar pronta (cerca de 15-20 segundos).

3.  **Configure o `application.properties`:** Verifique se o `application.properties` do serviço que você quer rodar (ex: `services/scheduler-service/src/main/resources/application.properties`) está apontando para o `localhost` nas portas corretas:

      * `spring.datasource.url=jdbc:postgresql://localhost:5438/scheduler_db`
      * `spring.kafka.bootstrap-servers=localhost:9092`

4.  **Execute** a classe `main` do serviço (ex: `SchedulerServiceApplication`) pela sua IDE.

## 📍 Endereços (Endpoints)

Quando a stack completa está de pé (`docker-compose up`):

| Serviço | Endereço Local | Descrição |
| :--- | :--- | :--- |
| **Scheduler Service** | `http://localhost:8081` | Serviço Core (API Principal) |
| ↳ GraphQL Playground | `http://localhost:8081/graphiql` | Interface para testar a API GraphQL |
| **Notification Service** | `http://localhost:8082` | Serviço de Notificações |
| **History Service** | `http://localhost:8083` | Serviço de Histórico (Read Model) |
| ↳ GraphQL Playground | `http://localhost:8083/graphiql` | Interface para consultar o histórico |
| **Kafka UI** | `http://localhost:8080` | UI Web para visualizar tópicos do Kafka |
| **PostgreSQL** | `localhost:5438` | Porta do banco para conectar via DBeaver/DataGrip |
| **Kafka** | `localhost:9092` | Porta do broker Kafka para produtores/consumidores locais |

-----

# 🏥 ClinicFiapApp - Sistema de Gestão Hospitalar Distribuído

**Tech Challenge - Fase 3 | Pós-Tech Arquitetura e Desenvolvimento Java**

Este repositório contém a solução backend para o sistema **ClinicFiapApp**, projetado para modernizar o agendamento de consultas, unificar históricos médicos e reduzir o absenteísmo através de notificações automáticas.

A solução adota uma arquitetura de **Microsserviços em Monorepo**, utilizando padrões avançados como **Transactional Outbox**, **CQRS (Leitura/Escrita)** e **Idempotência** para garantir consistência e resiliência.

---

## 📋 Índice

1. [Visão Geral da Arquitetura](#-visão-geral-da-arquitetura)
2. [Estrutura do Monorepo](#-estrutura-do-monorepo)
3. [Tecnologias Utilizadas](#-tecnologias-utilizadas)
4. [Microsserviços](#-microsserviços)
5. [Destaques de Qualidade (Nível Sênior)](#-destaques-de-qualidade-nível-sênior)
6. [Endpoints e API (GraphQL)](#-endpoints-e-api-graphql)
7. [Como Executar o Projeto](#-como-executar-o-projeto)
8. [Testes Automatizados](#-testes-automatizados)

---

## 🏛️ Visão Geral da Arquitetura

O sistema é composto por três serviços principais que se comunicam de forma híbrida: **Síncrona** (GraphQL/HTTP) para operações do usuário e **Assíncrona** (Kafka) para consistência eventual e notificações.

### Pilares da Arquitetura

* **Padrão Outbox:** Garante que nenhum evento de agendamento seja perdido, mesmo se o Kafka estiver indisponível. Eventos são salvos na mesma transação do banco (tabela `outbox_events`).
* **Segurança Assimétrica (RS256):** O *Scheduler Service* assina tokens com **Chave Privada**, enquanto os demais serviços validam com **Chave Pública**, garantindo que chaves públicas vazadas não comprometem a segurança.
* **Idempotência:** Consumidores Kafka preparados para processar mensagens duplicadas sem gerar inconsistência de dados ou envio múltiplo de e-mails.

---

## 📁 Estrutura do Monorepo

Gerenciada pelo Maven com bibliotecas compartilhadas e isolamento por serviço:

* **`pom.xml` (Raiz):** POM Agregador que gerencia todos os módulos e dependências.
* **`infra/`:** Scripts de inicialização (criação automática de bancos de dados).
* **`libs/outbox-relay/`:** (Futuro) Biblioteca compartilhada para DTOs e lógicas do padrão Outbox.
* **`services/scheduler-service/`:** Core da aplicação (usuários, autenticação, agendamentos, eventos).
* **`services/notification-service/`:** Consumidor Kafka para envio de notificações e e-mails transacionais.
* **`services/history-service/`:** Read Model (CQRS) para consultas otimizadas do histórico.

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Java 21
* **Framework:** Spring Boot 3.5.7
* **Banco de Dados:** PostgreSQL 17 (Containers isolados por serviço)
* **Mensageria:** Apache Kafka (Confluent 7.6.1)
* **API:** Spring for GraphQL
* **Segurança:** Spring Security + JWT (RSA 2048-bit)
* **Migração de Dados:** Flyway
* **Jobs Distribuídos:** ShedLock (com JDBC)
* **Testes de Integração:** Testcontainers + JUnit 5
* **Infraestrutura:** Docker & Docker Compose
* **Build:** Maven

---

## 📦 Microsserviços

### 1. `scheduler-service` (Core & Auth)
O "cérebro" da aplicação.

* **Responsabilidades:** 
  * Gestão de usuários (Médicos, Pacientes, Enfermeiros)
  * Autenticação e autorização (Spring Security + JWT RS256)
  * Lógica de Agendamento (conflitos de horário, regras de negócio)
  * Publicação de Eventos via Outbox Pattern
* **API:** GraphQL
* **Banco:** `scheduler_db`

### 2. `notification-service` (Consumer)
Serviço reativo focado em comunicação.

* **Responsabilidades:** 
  * Consumir eventos de agendamento (`AppointmentCreated`, `AppointmentConfirmed`)
  * Enviar e-mails transacionais
  * Gerenciar retentativas e Dead Letter Topics (DLT)
* **Features:** Retry automático, idempotência garantida por constraint única `(appointment_id, type)`
* **Banco:** `notification_db`

### 3. `history-service` (Read Model / CQRS)
Serviço de consulta otimizada.

* **Responsabilidades:** 
  * Consumir eventos para construir projeção unificada do histórico do paciente
  * Permitir consultas rápidas sem impactar o serviço de agendamento
  * Manter controle de processamento via tabela `processed_kafka_events`
* **API:** GraphQL (Consulta de Histórico)
* **Banco:** `history_db`

---

## 💎 Destaques de Qualidade (Nível Sênior)

Este projeto implementa padrões de arquitetura de referência:

1. **Transactional Outbox Pattern:**
   * Evita "Dual Write" salvando eventos na mesma transação do banco
   * `OutboxRelayService` publica eventos no Kafka de forma segura e confiável

2. **Segurança JWT com RS256 (Assimétrica):**
   * Chave Privada no `scheduler-service` para assinar tokens
   * Chave Pública distribuída para validação (sem risco se vazar)

3. **Consumidores Idempotentes:**
   * **Notification:** Constraint `(appointment_id, type)` previne duplicação
   * **History:** Tabela `processed_kafka_events` evita registros duplicados

4. **Concorrência Segura (ShedLock):**
   * Jobs distribuídos executados por apenas uma instância simultânea
   * Garante consistência em ambiente multi-réplica

5. **Testes Fidedignos (Testcontainers):**
   * Testes de integração com containers reais (PostgreSQL, Kafka)
   * Código testado reflete exatamente comportamento em produção

---

## 🔌 Endpoints e API (GraphQL)

A API é documentada e explorável via **GraphiQL** em `http://localhost:8081/graphiql` (Scheduler) e `http://localhost:8083/graphiql` (History).

| Operação | Tipo | Acesso | Descrição |
| :--- | :--- | :--- | :--- |
| `login` | Mutation | Público | Autentica usuário e retorna JWT |
| `createAppointment` | Mutation | Nurse/Doctor | Agenda nova consulta com validações |
| `confirmAppointment` | Mutation | Nurse/Doctor | Confirma consulta e dispara notificação |
| `appointments` | Query | Autenticado | Lista consultas (com filtro por role) |
| `history` | Query | Autenticado | Consulta histórico unificado (via History Service) |

### Exemplo de Mutation (Agendar):
```graphql
mutation {
  createAppointment(input: {
    patientId: "uuid-paciente",
    doctorId: "uuid-medico",
    startAt: "2025-12-10T10:00:00-03:00",
    endAt: "2025-12-10T11:00:00-03:00"
  }) {
    id
    status
  }
}
```

---

## 🚀 Como Executar o Projeto

### Pré-requisitos

* Docker e Docker Compose instalados
* Portas **5438, 9092, 2181, 8080, 8081, 8082, 8083** livres
* (Opcional) OpenSSL ou Java para gerar chaves RSA

### Passo 1: Preparação (Chaves de Segurança)

Como usamos criptografia robusta (RSA 2048-bit):

1. Crie um arquivo `.env` na raiz do projeto (baseado em `.env.example`)
2. Gere o par de chaves:
   - **Opção A (Java):** Execute a classe utilitária `KeyGen` localizada em `src/test/.../SchedulerServiceApplicationTests.java`
   - **Opção B (OpenSSL):** Use comandos padrão para gerar chaves RSA
3. Preenchao arquivo `.env`:
   ```properties
   JWT_PRIVATE_KEY=... (conteúdo da chave privada)
   JWT_PUBLIC_KEY=... (conteúdo da chave pública)
   ```

### Passo 2: Execução com Docker Compose

Na raiz do projeto, execute:

```bash
docker-compose up -d --build
```

**O que este comando faz:**

1. Sobe PostgreSQL, Kafka, Zookeeper e Kafka UI
2. Cria automaticamente os bancos (`scheduler_db`, `notification_db`, `history_db`)
3. Compila e inicia os 3 microsserviços
4. Garante resiliência com `restart: on-failure` para dependências

### Passo 3: Acessar a Aplicação

Após inicialização (1-2 minutos):

| Serviço | URL | Descrição |
| :--- | :--- | :--- |
| **Scheduler GraphQL** | http://localhost:8081/graphiql | API Principal |
| **History GraphQL** | http://localhost:8083/graphiql | Consultas de Histórico |
| **Kafka UI** | http://localhost:8080 | Visualização de Tópicos |
| **PostgreSQL** | localhost:5438 | Banco de dados (DBeaver/DataGrip) |

### Passo 4: Desenvolvimento Local (Rodando pela IDE)

Se preferir rodar um serviço pela IDE para debug:

1. Inicie apenas a infraestrutura:
   ```bash
   docker-compose up -d postgres zookeeper kafka kafka-ui
   ```

2. Aguarde 15-20 segundos para a infraestrutura ficar pronta

3. Configure `application.properties` do serviço (ex: `services/scheduler-service/src/main/resources/application.properties`):
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5438/scheduler_db
   spring.kafka.bootstrap-servers=localhost:9092
   ```

4. Execute a classe `main` do serviço (ex: `SchedulerServiceApplication`) pela sua IDE

### Comandos Úteis

```bash
# Parar a execução
docker-compose down

# Resetar bancos (apagar volumes)
docker-compose down -v

# Ver logs de um serviço
docker-compose logs -f scheduler-service

# Executar apenas um serviço
docker-compose up scheduler-service
```

---

## 🧪 Testes Automatizados

O projeto conta com suíte robusta de testes de integração usando Testcontainers.

Para executar:

```bash
./mvnw test
```

**Cobertura de Testes:**

* ✅ Fluxo completo de Autenticação (Login, Roles, JWT)
* ✅ Ciclo de vida do Agendamento (Criação, Confirmação, Cancelamento)
* ✅ Gestão de Usuários (CRUD de Pacientes, Médicos, Enfermeiros)
* ✅ Validação de Regras de Negócio (Horário comercial, conflitos de agenda)
* ✅ Idempotência de Consumidores Kafka
* ✅ Padrão Outbox e Processamento de Eventos

---

## 📍 Arquitetura de Rede

```
┌─────────────────────────────────────────────────────────────┐
│                    Cliente / Frontend                        │
└────────────────────────────┬────────────────────────────────┘
                             │
                    ┌────────┴────────┐
                    │   GraphQL API   │
                    └────────┬────────┘
         ┌──────────────────┼──────────────────┐
         │                  │                  │
    ┌────▼───────┐  ┌──────▼──────┐  ┌───────▼────┐
    │  Scheduler │  │ Notification│  │  History   │
    │  Service   │  │  Service    │  │  Service   │
    └────┬───────┘  └──────┬──────┘  └───────┬────┘
         │                 │                 │
    ┌────▼─────────────────▼─────────────────▼────┐
    │            Apache Kafka                     │
    │  (AppointmentCreated, Confirmed, etc)      │
    └────┬─────────────────────────────────────────┘
         │
    ┌────▼─────────────────────────────────────┐
    │      PostgreSQL (3 bancos isolados)      │
    │  scheduler_db | notification_db | history_db │
    └──────────────────────────────────────────┘
```

---

**Curso:** Pós-Tech Arquitetura e Desenvolvimento Java (FIAP)  
**Versão:** 1.0.0

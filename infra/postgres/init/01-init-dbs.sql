-- 01-init-dbs.sql
-- Este script é executado automaticamente pelo contêiner Postgres na primeira inicialização.
-- Ele cria os bancos de dados necessários para cada microsserviço.
-- O usuário (clinicfiap) já foi criado pela variável de ambiente POSTGRES_USER.

CREATE DATABASE scheduler_db;
CREATE DATABASE notification_db;
CREATE DATABASE history_db;

-- Opcional, mas boa prática: Define o usuário 'clinicfiap' como dono dos bancos
ALTER DATABASE scheduler_db OWNER TO clinicfiap;
ALTER DATABASE notification_db OWNER TO clinicfiap;
ALTER DATABASE history_db OWNER TO clinicfiap;
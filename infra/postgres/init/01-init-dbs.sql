-- 01-init-dbs.sql
-- Este script é executado automaticamente pelo contêiner Postgres na primeira inicialização.
-- Ele cria os bancos de dados necessários para cada microsserviço.
-- O usuário (clinicfiapp) já foi criado pela variável de ambiente POSTGRES_USER.

CREATE DATABASE scheduler_db;
CREATE DATABASE notification_db;
CREATE DATABASE history_db;

-- Opcional, mas boa prática: Define o usuário 'clinicfiapp' como dono dos bancos
ALTER DATABASE scheduler_db OWNER TO clinicfiapp;
ALTER DATABASE notification_db OWNER TO clinicfiapp;
ALTER DATABASE history_db OWNER TO clinicfiapp;

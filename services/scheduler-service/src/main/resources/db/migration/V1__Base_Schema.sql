-- V1__Base_Schema.sql (Domínio do Scheduler Service)
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       login VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL,
                       is_active BOOLEAN DEFAULT true,
                       created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctors (
                         user_id UUID PRIMARY KEY REFERENCES users(id),
                         crm VARCHAR(50) NOT NULL UNIQUE,
                         specialty VARCHAR(100) NOT NULL,
                         is_active BOOLEAN DEFAULT true
);

CREATE TABLE nurses (
                        user_id UUID PRIMARY KEY REFERENCES users(id),
                        is_active BOOLEAN DEFAULT true
);

CREATE TABLE patients (
                          user_id UUID PRIMARY KEY REFERENCES users(id),
                          birth_date DATE,
                          is_active BOOLEAN DEFAULT true
);

CREATE TABLE appointments (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              patient_id UUID NOT NULL REFERENCES patients(user_id),
                              doctor_id UUID NOT NULL REFERENCES doctors(user_id),
                              start_at TIMESTAMPTZ NOT NULL,
                              end_at TIMESTAMPTZ NOT NULL,
                              status VARCHAR(50) NOT NULL,
                              created_by UUID NOT NULL REFERENCES users(id),
                              is_active BOOLEAN DEFAULT true,
                              created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE appointments_history (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      appointment_id UUID NOT NULL REFERENCES appointments(id),
                                      action VARCHAR(100) NOT NULL,
                                      snapshot JSONB NOT NULL,
                                      event_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Tabela do Padrão OUTBOX
CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               aggregate_type VARCHAR(255) NOT NULL,
                               aggregate_id VARCHAR(255) NOT NULL,
                               event_type VARCHAR(255) NOT NULL,
                               payload JSONB NOT NULL,
                               processed BOOLEAN DEFAULT false,
                               created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
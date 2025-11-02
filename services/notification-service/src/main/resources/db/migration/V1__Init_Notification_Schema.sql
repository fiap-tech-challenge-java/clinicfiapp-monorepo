-- V1__Init_Notification_Schema.sql (Domínio do Notification Service)
CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               appointment_id UUID NOT NULL,
                               patient_id UUID NOT NULL,
                               channel VARCHAR(50) NOT NULL,
                               status VARCHAR(50) NOT NULL,
                               attempts INT DEFAULT 0,
                               scheduled_for TIMESTAMPTZ NOT NULL,
                               sent_at TIMESTAMPTZ,
                               created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
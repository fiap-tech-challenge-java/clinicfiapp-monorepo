-- V1__Init_History_Schema.sql (Domínio do History Service - Read Model)
CREATE TABLE projected_appointment_history (
                                               id UUID PRIMARY KEY,
                                               patient_id UUID NOT NULL,
                                               doctor_id UUID NOT NULL,
                                               doctor_name VARCHAR(255),
                                               patient_name VARCHAR(255),
                                               start_at TIMESTAMPTZ NOT NULL,
                                               status VARCHAR(50) NOT NULL,
                                               last_action VARCHAR(100),
                                               history_updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_history_patient_id ON projected_appointment_history(patient_id);
CREATE INDEX idx_history_doctor_id ON projected_appointment_history(doctor_id);
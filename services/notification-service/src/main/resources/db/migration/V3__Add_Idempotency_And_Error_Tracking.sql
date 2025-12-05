-- Migration para suportar Idempotência e Rastreamento de Erros

-- Adiciona coluna para rastrear o ID do evento
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS event_id VARCHAR(255);

-- Adiciona coluna para registrar o último erro
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS last_error VARCHAR(500);

-- Cria índice para busca rápida por event_id
CREATE INDEX IF NOT EXISTS idx_notifications_event_id ON notifications(event_id);

-- Adiciona constraint de unicidade para garantir IDEMPOTÊNCIA
-- Não permite duplicar notificações do mesmo tipo para o mesmo appointment
ALTER TABLE notifications
ADD CONSTRAINT uk_notification_idempotency
UNIQUE (appointment_id, notification_type, channel);

-- Adiciona comentários para documentação
COMMENT ON COLUMN notifications.event_id IS 'ID único do evento Kafka para rastreamento';
COMMENT ON COLUMN notifications.last_error IS 'Última mensagem de erro caso o envio tenha falhado';
COMMENT ON CONSTRAINT uk_notification_idempotency ON notifications IS 'Garante idempotência - não permite reenvio de notificações já processadas';


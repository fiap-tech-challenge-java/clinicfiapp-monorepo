-- Adiciona coluna notification_type na tabela notifications
ALTER TABLE notifications
ADD COLUMN notification_type VARCHAR(50) NOT NULL DEFAULT 'LEMBRETE';

-- Remove o default após adicionar a coluna
ALTER TABLE notifications
ALTER COLUMN notification_type DROP DEFAULT;

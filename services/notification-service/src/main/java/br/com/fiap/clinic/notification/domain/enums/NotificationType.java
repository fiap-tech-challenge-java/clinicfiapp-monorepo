package br.com.fiap.clinic.notification.domain.enums;

public enum NotificationType {
    APPOINTMENT_REMINDER("Lembrete de Consulta"),
    APPOINTMENT("Confirmação de Agendamento");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}


package br.com.fiap.clinic.notification.domain.enums;

public enum Status {
    PENDING("Pendente de envio"),
    SENT("Enviado"),
    FAILED("Falha no envio");
    
    private final String description;

    Status(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

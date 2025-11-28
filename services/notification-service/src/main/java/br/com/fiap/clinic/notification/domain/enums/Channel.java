package br.com.fiap.clinic.notification.domain.enums;

public enum Channel {
    EMAIL("Envio feito por E-mail"),
    SMS("Envio feito por SMS");

    private final String description;

    Channel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

package br.com.fiap.clinic.scheduler.domain.entity;

public enum Role {
    MEDICO("MEDICO"),
    ENFERMEIRO("ENFERMEIRO"),
    PACIENTE("PACIENTE");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}
package br.com.fiap.clinic.notification.dto;

public record AppointmentEvent(
        Long appointmentId,
        String patientEmail,
        String date,
        String time
) {
}

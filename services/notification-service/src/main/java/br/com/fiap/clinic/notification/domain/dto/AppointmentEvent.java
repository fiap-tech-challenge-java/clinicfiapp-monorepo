package br.com.fiap.clinic.notification.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentEvent(
        UUID appointmentId,
        UUID patientId,
        String patientName,
        String patientEmail,
        String doctorId,
        String doctorName,
        String doctorSpecialty,
        String appointmentDate
) {
}
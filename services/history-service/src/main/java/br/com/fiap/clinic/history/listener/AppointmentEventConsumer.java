package br.com.fiap.clinic.history.listener;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentEventConsumer {
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private String doctorName;
    private String patientName;
    private String patientEmail;
    private String status;
    private String eventType;
    private String timestamp;
}

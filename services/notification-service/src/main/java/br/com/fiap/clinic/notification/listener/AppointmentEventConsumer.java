package br.com.fiap.clinic.notification.listener;

import br.com.fiap.clinic.notification.domain.service.NotificationHandlerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventConsumer {

    private final NotificationHandlerService notificationHandlerService;
    private final ObjectMapper objectMapper;

    /**
     * Escuta eventos de lembretes de consulta vindos do Kafka
     */
    @KafkaListener(topics = "appointment-events", groupId = "notification-consumers")
    public void consumeAppointmentEvent(String message) {
        try {
            log.info("Recebido evento de consulta: {}", message);

            JsonNode eventNode = objectMapper.readTree(message);
            String eventType = eventNode.get("eventType").asText();

            if ("AppointmentReminderRequested".equals(eventType)) {
                processReminderEvent(eventNode.get("payload"));
            } else if ("AppointmentCreated".equals(eventType) || "AppointmentScheduled".equals(eventType)) {
                processConfirmationEvent(eventNode.get("payload"));
            }

        } catch (Exception e) {
            log.error("Erro ao processar evento de consulta", e);
        }
    }

    /**
     * Processa o evento de lembrete
     */
    private void processReminderEvent(JsonNode payload) {
        try {
            UUID appointmentId = UUID.fromString(payload.get("appointmentId").asText());
            UUID patientId = UUID.fromString(payload.get("patientId").asText());
            String patientName = payload.get("patientName").asText();
            String patientEmail = payload.get("patientEmail").asText();
            String doctorName = payload.get("doctorName").asText();
            String doctorSpecialty = payload.get("doctorSpecialty").asText();
            LocalDateTime appointmentDate = LocalDateTime.parse(payload.get("appointmentDate").asText());

            notificationHandlerService.handleAppointmentReminder(
                    appointmentId,
                    patientId,
                    patientName,
                    patientEmail,
                    doctorName,
                    doctorSpecialty,
                    appointmentDate
            );

        } catch (Exception e) {
            log.error("Erro ao processar evento de lembrete", e);
        }
    }

    /**
     * Processa o evento de confirmação de agendamento
     */
    private void processConfirmationEvent(JsonNode payload) {
        try {
            UUID appointmentId = UUID.fromString(payload.get("appointmentId").asText());
            UUID patientId = UUID.fromString(payload.get("patientId").asText());
            String patientName = payload.get("patientName").asText();
            String patientEmail = payload.get("patientEmail").asText();
            String doctorName = payload.get("doctorName").asText();
            String doctorSpecialty = payload.get("doctorSpecialty").asText();
            LocalDateTime appointmentDate = LocalDateTime.parse(payload.get("appointmentDate").asText());

            notificationHandlerService.handleAppointmentConfirmation(
                    appointmentId,
                    patientId,
                    patientName,
                    patientEmail,
                    doctorName,
                    doctorSpecialty,
                    appointmentDate
            );

        } catch (Exception e) {
            log.error("Erro ao processar evento de confirmação", e);
        }
    }
}

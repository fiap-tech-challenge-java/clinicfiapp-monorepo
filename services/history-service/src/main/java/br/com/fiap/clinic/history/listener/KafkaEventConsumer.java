package br.com.fiap.clinic.history.listener;

import br.com.fiap.clinic.history.config.KafkaConfig;
import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.repository.ProcessedEventRepository;
import br.com.fiap.clinic.history.domain.service.HistoryProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final HistoryProjectionService historyService;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = KafkaConfig.TOPIC_NAME, groupId = "history-consumers")
    public void listen(AppointmentEventConsumer event) {
        try {

            if (event.getEventId() != null) {
                UUID eventUuid = UUID.fromString(event.getEventId());
                if (processedEventRepository.existsById(eventUuid)) {
                    log.warn("Evento {} já foi processado anteriormente. Ignorando duplicação.", eventUuid);
                    return;
                }
            } else {
                log.warn("Evento recebido sem eventId! Processando sem garantia de idempotência.");
            }


            log.info("Recebido evento do tópico {}: patientId={}, doctorId={}, eventType={}",
                    KafkaConfig.TOPIC_NAME, event.getPatientId(), event.getDoctorId(), event.getEventType());

            if (event.getPatientId() == null || event.getPatientId().isEmpty()) {
                log.warn("Evento ignorado: patientId é obrigatório");
                return;
            }

            if (event.getDoctorId() == null || event.getDoctorId().isEmpty()) {
                log.warn("Evento ignorado: doctorId é obrigatório");
                return;
            }

            UUID patientUuid;
            UUID doctorUuid;

            try {
                patientUuid = UUID.fromString(event.getPatientId());
            } catch (IllegalArgumentException e) {
                log.error("Erro ao converter patientId '{}' para UUID", event.getPatientId(), e);
                return;
            }

            try {
                doctorUuid = UUID.fromString(event.getDoctorId());
            } catch (IllegalArgumentException e) {
                log.error("Erro ao converter doctorId '{}' para UUID", event.getDoctorId(), e);
                return;
            }

            ProjectedAppointmentHistory history = new ProjectedAppointmentHistory();
            history.setPatientId(patientUuid);
            history.setDoctorId(doctorUuid);
            history.setDoctorName(event.getDoctorName());
            history.setPatientName(event.getPatientName());
            history.setStatus(event.getStatus());
            history.setLastAction(event.getEventType());

            if (event.getAppointmentDate() != null && !event.getAppointmentDate().isEmpty()) {
                try {
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(event.getAppointmentDate(), DateTimeFormatter.ISO_DATE_TIME);
                    ZonedDateTime brasiliaDateTime = offsetDateTime.atZoneSameInstant(ZoneId.of("America/Sao_Paulo"));
                    history.setStartAt(brasiliaDateTime.toOffsetDateTime());
                } catch (DateTimeParseException e) {
                    log.error("Erro ao parsear appointmentDate '{}'. Evento ignorado.", event.getAppointmentDate(), e);
                    return;
                }
            } else {
                log.error("appointmentDate é obrigatório. Evento ignorado.");
                return;
            }

            historyService.createHistoryFromKafka(history);

            log.info("Histórico salvo com sucesso: patientId={}, doctorId={}, startAt={}",
                    event.getPatientId(), event.getDoctorId(), history.getStartAt());

        } catch (Exception e) {
            log.error("Erro ao processar mensagem do Kafka: patientId={}, doctorId={}, eventType={}",
                    event.getPatientId(), event.getDoctorId(), event.getEventType(), e);
        }
    }
}
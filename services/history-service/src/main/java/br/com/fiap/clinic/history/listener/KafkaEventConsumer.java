package br.com.fiap.clinic.history.listener;

import br.com.fiap.clinic.history.config.KafkaConfig;
import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.service.HistoryProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final HistoryProjectionService historyService;

    @KafkaListener(topics = KafkaConfig.TOPIC_NAME, groupId = "history-consumers")
    public void listen(AppointmentEventConsumer event) {
        try {
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

            ProjectedAppointmentHistory history = new ProjectedAppointmentHistory();
            history.setPatientId(event.getPatientId());
            history.setDoctorId(event.getDoctorId());
            history.setDoctorName(event.getDoctorName());
            history.setPatientName(event.getPatientName());
            history.setStatus(event.getStatus());
            history.setLastAction(event.getEventType());

            if (event.getTimestamp() != null && !event.getTimestamp().isEmpty()) {
                try {
                    history.setStartAt(LocalDateTime.parse(event.getTimestamp(), DateTimeFormatter.ISO_DATE_TIME));
                } catch (DateTimeParseException e) {
                    log.warn("Erro ao parsear timestamp '{}'. Usando LocalDateTime.now()", event.getTimestamp());
                    history.setStartAt(LocalDateTime.now());
                }
            } else {
                history.setStartAt(LocalDateTime.now());
            }

            historyService.createHistoryFromKafka(history);

            log.info("Histórico salvo com sucesso: patientId={}, doctorId={}",
                event.getPatientId(), event.getDoctorId());

        } catch (Exception e) {
            log.error("Erro ao processar mensagem do Kafka: patientId={}, doctorId={}, eventType={}",
                event.getPatientId(), event.getDoctorId(), event.getEventType(), e);
        }
    }
}
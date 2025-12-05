package br.com.fiap.clinic.notification.listener;

import br.com.fiap.clinic.notification.domain.dto.AppointmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Consumidor da Dead Letter Topic (DLT)
 * Registra mensagens que falharam após todas as tentativas de retry
 * TODO: Armazenar em tabela separada para análise posterior
 */
@Service
@Slf4j
public class DeadLetterQueueConsumer {

    @KafkaListener(
            topics = "appointment-events-dlt",
            groupId = "notification-dlt-consumers",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleDeadLetter(
            @Payload AppointmentEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.error("💀 DEAD LETTER TOPIC: Mensagem recebida no DLT após falha em todas as tentativas");
        log.error("💀 Appointment ID: {}", event.appointmentId());
        log.error("💀 Patient: {} ({})", event.patientName(), event.patientEmail());
        log.error("💀 Doctor: {}", event.doctorName());
        log.error("💀 Date: {}", event.appointmentDate());
        log.error("💀 Partition: {}, Offset: {}", partition, offset);
        log.error("💀 AÇÃO NECESSÁRIA: Verificar logs anteriores para identificar a causa da falha");
        log.error("💀 Evento completo: {}", event);

        // TODO: Salvar em tabela de eventos falhados para reprocessamento manual
    }
}


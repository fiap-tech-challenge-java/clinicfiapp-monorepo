package br.com.fiap.clinic.notification.listener;

import br.com.fiap.clinic.notification.domain.service.NotificationHandlerService;
import br.com.fiap.clinic.notification.domain.dto.AppointmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AppointmentEventConsumer {

    private final NotificationHandlerService notificationHandlerService;

    public AppointmentEventConsumer(NotificationHandlerService notificationHandlerService) {
        this.notificationHandlerService = notificationHandlerService;
    }

    @KafkaListener(
            topics = "appointment-events",
            groupId = "notification-consumers",
            containerFactory = "kafkaListenerContainerFactory")
    public void handler(
            @Payload AppointmentEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("📨 Mensagem Kafka recebida - Appointment ID: {}, Patient: {}, Partition: {}, Offset: {}",
                event.appointmentId(), event.patientName(), partition, offset);

        try {
            notificationHandlerService.handleAppointmentConfirmation(event);
            log.info("✅ Evento processado com sucesso - Appointment ID: {}", event.appointmentId());
        } catch (Exception e) {
            log.error("❌ Erro ao processar evento - Appointment ID: {} - Erro: {}",
                    event.appointmentId(), e.getMessage());
            // Propaga exceção para o ErrorHandler fazer retry ou enviar para DLQ
            throw e;
        }
    }
}

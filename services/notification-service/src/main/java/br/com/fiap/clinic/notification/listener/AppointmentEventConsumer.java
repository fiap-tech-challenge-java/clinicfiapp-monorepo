package br.com.fiap.clinic.notification.listener;

import br.com.fiap.clinic.notification.domain.service.NotificationHandlerService;
import br.com.fiap.clinic.notification.dto.AppointmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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
    public void handler(AppointmentEvent event) {
        log.info("Mensagem Kafka recebida: {}", event);

        notificationHandlerService.handleAppointmentConfirmation(event);
    }
}

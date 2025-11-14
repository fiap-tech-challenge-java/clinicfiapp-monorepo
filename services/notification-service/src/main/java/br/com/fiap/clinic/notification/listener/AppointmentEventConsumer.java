package br.com.fiap.clinic.notification.listener;

import br.com.fiap.clinic.notification.dto.AppointmentEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AppointmentEventConsumer {

    @KafkaListener(
            topics = "appointments",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAppointmentEvent(AppointmentEvent event) {
        System.out.println("Recebido evento: " + event);
    }

}

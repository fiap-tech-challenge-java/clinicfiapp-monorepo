package br.com.fiap.clinic.notification.domain.repository;

import br.com.fiap.clinic.notification.domain.entity.Notification;
import br.com.fiap.clinic.notification.domain.enums.Channel;
import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Verifica se já existe uma notificação
     */
    boolean existsByAppointmentIdAndNotificationTypeAndChannel(
            UUID appointmentId,
            NotificationType notificationType,
            Channel channel
    );

    /**
     * Busca notificação existente para atualização
     */
    Optional<Notification> findByAppointmentIdAndNotificationTypeAndChannel(
            UUID appointmentId,
            NotificationType notificationType,
            Channel channel
    );

    /**
     * Busca por eventId para rastreamento
     */
    Optional<Notification> findByEventId(String eventId);
}

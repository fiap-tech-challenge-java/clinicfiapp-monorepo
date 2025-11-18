package br.com.fiap.clinic.notification.domain.entity;

import br.com.fiap.clinic.notification.domain.enums.Channel;
import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import br.com.fiap.clinic.notification.domain.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private Status status;

    @Column
    private Integer attempts = 0;

    @Column(name = "scheduled_for", nullable = false)
    private LocalDateTime scheduledFor;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public NotificationEntity(UUID appointmentId, UUID patientId, NotificationType notificationType,
                              Channel channel, Status status, LocalDateTime scheduledFor) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.status = status;
        this.scheduledFor = scheduledFor;
    }
}

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

@Entity
@Table(name = "notifications",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"appointment_id", "notification_type", "channel"},
           name = "uk_notification_idempotency"
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;


    @Enumerated(EnumType.STRING)
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

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "last_error")
    private String lastError;

    public Notification(UUID appointmentId, UUID patientId, NotificationType notificationType,
                              Channel channel, Status status, LocalDateTime scheduledFor) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.status = status;
        this.scheduledFor = scheduledFor;
    }
}

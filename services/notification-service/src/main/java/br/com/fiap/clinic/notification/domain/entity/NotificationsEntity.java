package br.com.fiap.clinic.notification.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Entity
@Table(name = "notifications")
public class NotificationsEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "attempts")
    private Integer attempts;

    @Column(name = "scheduled_for", nullable = false)
    private Timestamp scheduledFor;

    @Column(name = "sent_at")
    private Timestamp sentAt;

    @Column(name = "created_at")
    private Timestamp createdAt;
}

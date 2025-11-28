package br.com.fiap.clinic.scheduler.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments_history")
@Getter
@Setter
@NoArgsConstructor
public class AppointmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(nullable = false)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String snapshot;

    @Column(name = "event_time")
    private OffsetDateTime eventTime;

    @PrePersist
    protected void onPersist() {
        this.eventTime = OffsetDateTime.now();
    }
}
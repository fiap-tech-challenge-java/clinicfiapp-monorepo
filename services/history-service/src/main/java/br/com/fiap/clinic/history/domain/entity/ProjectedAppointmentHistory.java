package br.com.fiap.clinic.history.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "projected_appointment_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectedAppointmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "doctor_name", nullable = false)
    private String doctorName;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "status")
    private String status;

    @Column(name = "last_action")
    private String lastAction;

    @UpdateTimestamp
    @Column(name = "history_updated_at")
    private LocalDateTime historyUpdatedAt;
}

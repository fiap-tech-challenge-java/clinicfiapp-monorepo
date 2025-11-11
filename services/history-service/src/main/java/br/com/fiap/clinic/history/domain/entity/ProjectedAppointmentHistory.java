package br.com.fiap.clinic.history.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "projected_appointment_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectedAppointmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

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

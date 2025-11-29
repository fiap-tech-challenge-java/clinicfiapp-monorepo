package br.com.fiap.clinic.history.domain.repository;

import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectedAppointmentHistoryRepository extends JpaRepository<ProjectedAppointmentHistory, UUID> {

    // Já existem
    List<ProjectedAppointmentHistory> findByPatientId(UUID patientId);
    List<ProjectedAppointmentHistory> findByPatientNameContainingIgnoreCase(String patientName);

    // --- NOVOS ---

    // Busca por Médico
    List<ProjectedAppointmentHistory> findByDoctorId(UUID doctorId);

    // Busca por Data (Intervalo do dia)
    List<ProjectedAppointmentHistory> findByStartAtBetween(LocalDateTime start, LocalDateTime end);

    // Busca Combinada (Opcional, mas útil): Médico + Data
    List<ProjectedAppointmentHistory> findByDoctorIdAndStartAtBetween(UUID doctorId, LocalDateTime start, LocalDateTime end);

    // Busca Dinâmica (Query Customizada para flexibilidade)
    // Isso permite filtrar por N campos ao mesmo tempo se eles não forem nulos
    @Query("SELECT p FROM ProjectedAppointmentHistory p WHERE " +
            "(:patientId IS NULL OR p.patientId = :patientId) AND " +
            "(:doctorId IS NULL OR p.doctorId = :doctorId) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:start IS NULL OR p.startAt >= :start) AND " +
            "(:end IS NULL OR p.startAt <= :end)")
    List<ProjectedAppointmentHistory> findCustom(
            @Param("patientId") UUID patientId,
            @Param("doctorId") UUID doctorId,
            @Param("status") String status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
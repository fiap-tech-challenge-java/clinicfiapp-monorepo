package br.com.fiap.clinic.history.domain.repository;

import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectedAppointmentHistoryRepository extends JpaRepository<ProjectedAppointmentHistory, UUID> {
    List<ProjectedAppointmentHistory> findByPatientId(UUID patientId);

    List<ProjectedAppointmentHistory> findByPatientNameContainingIgnoreCase(String patientName);
}

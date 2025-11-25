package br.com.fiap.clinic.history.domain.repository;

import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectedAppointmentHistoryRepository extends JpaRepository<ProjectedAppointmentHistory, Long> {
    List<ProjectedAppointmentHistory> findByPatientId(Long patientId);
    List<ProjectedAppointmentHistory> findByDoctorId(Long doctorId);
}

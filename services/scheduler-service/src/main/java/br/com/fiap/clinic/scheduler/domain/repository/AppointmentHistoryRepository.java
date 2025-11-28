package br.com.fiap.clinic.scheduler.domain.repository;

import br.com.fiap.clinic.scheduler.domain.entity.AppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, UUID> {
    List<AppointmentHistory> findByAppointmentIdOrderByEventTimeDesc(UUID appointmentId);
}
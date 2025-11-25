package br.com.fiap.clinic.scheduler.domain.repository;

import br.com.fiap.clinic.scheduler.domain.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /**
     * Busca consultas agendadas para o dia seguinte que estão ativas e confirmadas/agendadas
     */
    @Query("SELECT a FROM Appointment a " +
           "WHERE a.startAt BETWEEN :startDate AND :endDate " +
           "AND a.isActive = true " +
           "AND a.status IN ('SCHEDULED', 'CONFIRMED')")
    List<Appointment> findAppointmentsForReminder(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}

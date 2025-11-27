package br.com.fiap.clinic.scheduler.domain.repository;

import br.com.fiap.clinic.scheduler.domain.entity.Appointment;
import br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByStatusAndIsActiveTrue(AppointmentStatus status);

    List<Appointment> findByPatientUserIdAndIsActiveTrue(UUID patientId);
    List<Appointment> findByDoctorUserIdAndIsActiveTrue(UUID doctorId);

    @Query("SELECT a FROM Appointment a " +
            "WHERE a.startAt BETWEEN :startDate AND :endDate " +
            "AND a.isActive = true " +
            "AND a.status IN (br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus.SOLICITADO, br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus.CONFIRMADO)")
    List<Appointment> findAppointmentsForReminder(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

}
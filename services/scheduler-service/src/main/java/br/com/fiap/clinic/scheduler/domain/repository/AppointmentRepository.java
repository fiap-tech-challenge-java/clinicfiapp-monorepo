package br.com.fiap.clinic.scheduler.domain.repository;

import br.com.fiap.clinic.scheduler.domain.entity.Appointment;
import br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByStatusAndIsActiveTrue(AppointmentStatus status);

    List<Appointment> findByPatient_IdAndIsActiveTrue(UUID patientId);
    List<Appointment> findByDoctor_IdAndIsActiveTrue(UUID doctorId);

    @Query("SELECT a FROM Appointment a " +
            "WHERE a.startAt BETWEEN :startDate AND :endDate " +
            "AND a.isActive = true " +
            "AND a.status IN (br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus.SCHEDULED, br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus.CONFIRMED, br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus.RESCHEDULED)")
    List<Appointment> findAppointmentsForReminder(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
            "AND a.startAt < :endAt AND a.endAt > :startAt " +
            "AND a.status IN (br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus.SCHEDULED, br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus.CONFIRMED, br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus.RESCHEDULED) " +
            "AND a.isActive = true")
    List<Appointment> findDoctorConflictingAppointments(
            @Param("doctorId") UUID doctorId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId " +
            "AND a.startAt < :endAt AND a.endAt > :startAt " +
            "AND a.status IN (br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus.SCHEDULED, br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus.CONFIRMED, br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus.RESCHEDULED) " +
            "AND a.isActive = true")
    List<Appointment> findPatientConflictingAppointments(
            @Param("patientId") UUID patientId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

}
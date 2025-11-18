package br.com.fiap.clinic.scheduler.domain.repository;

import br.com.fiap.clinic.scheduler.domain.entity.Appointment;
import br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientUserIdAndIsActiveTrue(UUID patientId);

    List<Appointment> findByDoctorUserIdAndIsActiveTrue(UUID doctorId);

    List<Appointment> findByStatusAndIsActiveTrue(AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.userId = :doctorId " +
           "AND a.startAt < :endAt AND a.endAt > :startAt " +
           "AND a.status IN ('SOLICITADO', 'CONFIRMADO') " +
           "AND a.isActive = true")
    List<Appointment> findDoctorConflictingAppointments(
            @Param("doctorId") UUID doctorId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    @Query("SELECT a FROM Appointment a WHERE a.patient.userId = :patientId " +
           "AND a.startAt < :endAt AND a.endAt > :startAt " +
           "AND a.status IN ('SOLICITADO', 'CONFIRMADO') " +
           "AND a.isActive = true")
    List<Appointment> findPatientConflictingAppointments(
            @Param("patientId") UUID patientId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    Optional<Appointment> findByIdAndIsActiveTrue(UUID id);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.userId = :doctorId " +
           "AND a.startAt >= :startDate AND a.startAt < :endDate " +
           "AND a.isActive = true ORDER BY a.startAt")
    List<Appointment> findByDoctorAndDateRange(
            @Param("doctorId") UUID doctorId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query("SELECT a FROM Appointment a WHERE a.patient.userId = :patientId " +
           "AND a.startAt >= :startDate AND a.startAt < :endDate " +
           "AND a.isActive = true ORDER BY a.startAt")
    List<Appointment> findByPatientAndDateRange(
            @Param("patientId") UUID patientId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );
}
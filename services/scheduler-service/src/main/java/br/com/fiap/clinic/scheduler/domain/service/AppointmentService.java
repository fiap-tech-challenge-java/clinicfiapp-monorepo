package br.com.fiap.clinic.scheduler.domain.service;

import br.com.fiap.clinic.scheduler.domain.entity.*;
import br.com.fiap.clinic.scheduler.domain.repository.*;
import br.com.fiap.clinic.scheduler.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Cria um novo agendamento
     */
    @Transactional
    public Appointment createAppointment(UUID patientId, UUID doctorId, UUID createdByUserId,
                                         OffsetDateTime startAt, OffsetDateTime endAt) {
        log.info("Criando agendamento: paciente={}, médico={}, início={}, fim={}",
                 patientId, doctorId, startAt, endAt);

        // Validações básicas
        validateAppointmentTime(startAt, endAt);

        // Buscar entidades
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + patientId));

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado: " + doctorId));

        User createdBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + createdByUserId));

        // Validar se paciente e médico estão ativos
        if (!patient.isActive()) {
            throw new IllegalArgumentException("Paciente não está ativo");
        }
        if (!doctor.isActive()) {
            throw new IllegalArgumentException("Médico não está ativo");
        }

        // Verificar conflitos de horário
        checkDoctorAvailability(doctorId, startAt, endAt);
        checkPatientAvailability(patientId, startAt, endAt);

        // Criar o agendamento
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setCreatedBy(createdBy);
        appointment.setStartAt(startAt);
        appointment.setEndAt(endAt);
        appointment.setStatus(AppointmentStatus.SOLICITADO);
        appointment.setActive(true);

        appointment = appointmentRepository.save(appointment);
        log.info("Agendamento criado com sucesso: id={}", appointment.getId());

        // Salvar no histórico
        saveHistory(appointment, "CRIADO");

        // Publicar evento no outbox
        publishEvent(appointment, "AppointmentCreated");

        return appointment;
    }

    /**
     * Busca um agendamento por ID
     */
    @Transactional(readOnly = true)
    public Appointment findById(UUID id) {
        return appointmentRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado: " + id));
    }

    /**
     * Lista todos os agendamentos ativos
     */
    @Transactional(readOnly = true)
    public List<Appointment> findAll() {
        return appointmentRepository.findAll().stream()
                .filter(Appointment::isActive)
                .toList();
    }

    /**
     * Lista agendamentos de um paciente
     */
    @Transactional(readOnly = true)
    public List<Appointment> findByPatient(UUID patientId) {
        return appointmentRepository.findByPatientUserIdAndIsActiveTrue(patientId);
    }

    /**
     * Lista agendamentos de um médico
     */
    @Transactional(readOnly = true)
    public List<Appointment> findByDoctor(UUID doctorId) {
        return appointmentRepository.findByDoctorUserIdAndIsActiveTrue(doctorId);
    }

    /**
     * Lista agendamentos por status
     */
    @Transactional(readOnly = true)
    public List<Appointment> findByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatusAndIsActiveTrue(status);
    }

    /**
     * Lista agendamentos de um médico em um período
     */
    @Transactional(readOnly = true)
    public List<Appointment> findByDoctorAndDateRange(UUID doctorId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return appointmentRepository.findByDoctorAndDateRange(doctorId, startDate, endDate);
    }

    /**
     * Lista agendamentos de um paciente em um período
     */
    @Transactional(readOnly = true)
    public List<Appointment> findByPatientAndDateRange(UUID patientId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return appointmentRepository.findByPatientAndDateRange(patientId, startDate, endDate);
    }

    /**
     * Confirma um agendamento
     */
    @Transactional
    public Appointment confirmAppointment(UUID appointmentId) {
        log.info("Confirmando agendamento: id={}", appointmentId);

        Appointment appointment = findById(appointmentId);

        if (appointment.getStatus() != AppointmentStatus.SOLICITADO) {
            throw new IllegalStateException("Apenas agendamentos solicitados podem ser confirmados");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMADO);
        appointment = appointmentRepository.save(appointment);

        saveHistory(appointment, "CONFIRMADO");
        publishEvent(appointment, "AppointmentConfirmed");

        log.info("Agendamento confirmado com sucesso: id={}", appointmentId);
        return appointment;
    }

    /**
     * Cancela um agendamento
     */
    @Transactional
    public Appointment cancelAppointment(UUID appointmentId) {
        log.info("Cancelando agendamento: id={}", appointmentId);

        Appointment appointment = findById(appointmentId);

        if (appointment.getStatus() == AppointmentStatus.REALIZADO) {
            throw new IllegalStateException("Não é possível cancelar um agendamento já realizado");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELADO) {
            throw new IllegalStateException("Agendamento já está cancelado");
        }

        appointment.setStatus(AppointmentStatus.CANCELADO);
        appointment = appointmentRepository.save(appointment);

        saveHistory(appointment, "CANCELADO");
        publishEvent(appointment, "AppointmentCancelled");

        log.info("Agendamento cancelado com sucesso: id={}", appointmentId);
        return appointment;
    }

    /**
     * Marca um agendamento como realizado
     */
    @Transactional
    public Appointment completeAppointment(UUID appointmentId) {
        log.info("Marcando agendamento como realizado: id={}", appointmentId);

        Appointment appointment = findById(appointmentId);

        if (appointment.getStatus() != AppointmentStatus.CONFIRMADO) {
            throw new IllegalStateException("Apenas agendamentos confirmados podem ser marcados como realizados");
        }

        appointment.setStatus(AppointmentStatus.REALIZADO);
        appointment = appointmentRepository.save(appointment);

        saveHistory(appointment, "REALIZADO");
        publishEvent(appointment, "AppointmentCompleted");

        log.info("Agendamento marcado como realizado: id={}", appointmentId);
        return appointment;
    }

    /**
     * Reagenda um agendamento (altera horários)
     */
    @Transactional
    public Appointment rescheduleAppointment(UUID appointmentId, OffsetDateTime newStartAt, OffsetDateTime newEndAt) {
        log.info("Reagendando: id={}, novo início={}, novo fim={}", appointmentId, newStartAt, newEndAt);

        Appointment appointment = findById(appointmentId);

        if (appointment.getStatus() == AppointmentStatus.REALIZADO) {
            throw new IllegalStateException("Não é possível reagendar um agendamento já realizado");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELADO) {
            throw new IllegalStateException("Não é possível reagendar um agendamento cancelado");
        }

        // Validar novos horários
        validateAppointmentTime(newStartAt, newEndAt);

        // Verificar conflitos (excluindo o próprio agendamento)
        checkDoctorAvailability(appointment.getDoctor().getUserId(), newStartAt, newEndAt, appointmentId);
        checkPatientAvailability(appointment.getPatient().getUserId(), newStartAt, newEndAt, appointmentId);

        appointment.setStartAt(newStartAt);
        appointment.setEndAt(newEndAt);
        appointment = appointmentRepository.save(appointment);

        saveHistory(appointment, "REAGENDADO");
        publishEvent(appointment, "AppointmentRescheduled");

        log.info("Agendamento reagendado com sucesso: id={}", appointmentId);
        return appointment;
    }

    /**
     * Exclusão lógica de um agendamento
     */
    @Transactional
    public void deleteAppointment(UUID appointmentId) {
        log.info("Excluindo logicamente agendamento: id={}", appointmentId);

        Appointment appointment = findById(appointmentId);
        appointment.setActive(false);
        appointmentRepository.save(appointment);

        saveHistory(appointment, "EXCLUÍDO");
        publishEvent(appointment, "AppointmentDeleted");

        log.info("Agendamento excluído logicamente: id={}", appointmentId);
    }

    /**
     * Busca histórico de um agendamento
     */
    @Transactional(readOnly = true)
    public List<AppointmentHistory> findAppointmentHistory(UUID appointmentId) {
        return appointmentHistoryRepository.findByAppointmentIdOrderByEventTimeDesc(appointmentId);
    }

    // ==================== Métodos auxiliares ====================

    private void validateAppointmentTime(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt.isAfter(endAt)) {
            throw new IllegalArgumentException("A data de início deve ser anterior à data de fim");
        }

        if (startAt.isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Não é possível agendar para uma data passada");
        }

        // Validar duração mínima (ex: 15 minutos)
        if (startAt.plusMinutes(15).isAfter(endAt)) {
            throw new IllegalArgumentException("A duração mínima de um agendamento é 15 minutos");
        }
    }

    private void checkDoctorAvailability(UUID doctorId, OffsetDateTime startAt, OffsetDateTime endAt) {
        checkDoctorAvailability(doctorId, startAt, endAt, null);
    }

    private void checkDoctorAvailability(UUID doctorId, OffsetDateTime startAt, OffsetDateTime endAt, UUID excludeAppointmentId) {
        List<Appointment> conflicts = appointmentRepository.findDoctorConflictingAppointments(doctorId, startAt, endAt);

        if (excludeAppointmentId != null) {
            conflicts = conflicts.stream()
                    .filter(a -> !a.getId().equals(excludeAppointmentId))
                    .toList();
        }

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Médico já possui agendamento neste horário");
        }
    }

    private void checkPatientAvailability(UUID patientId, OffsetDateTime startAt, OffsetDateTime endAt) {
        checkPatientAvailability(patientId, startAt, endAt, null);
    }

    private void checkPatientAvailability(UUID patientId, OffsetDateTime startAt, OffsetDateTime endAt, UUID excludeAppointmentId) {
        List<Appointment> conflicts = appointmentRepository.findPatientConflictingAppointments(patientId, startAt, endAt);

        if (excludeAppointmentId != null) {
            conflicts = conflicts.stream()
                    .filter(a -> !a.getId().equals(excludeAppointmentId))
                    .toList();
        }

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Paciente já possui agendamento neste horário");
        }
    }

    private void saveHistory(Appointment appointment, String action) {
        try {
            AppointmentHistory history = new AppointmentHistory();
            history.setAppointment(appointment);
            history.setAction(action);

            // Criar snapshot do agendamento
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("id", appointment.getId().toString());
            snapshot.put("patientId", appointment.getPatient().getUserId().toString());
            snapshot.put("doctorId", appointment.getDoctor().getUserId().toString());
            snapshot.put("startAt", appointment.getStartAt().toString());
            snapshot.put("endAt", appointment.getEndAt().toString());
            snapshot.put("status", appointment.getStatus().toString());
            snapshot.put("isActive", appointment.isActive());

            history.setSnapshot(objectMapper.writeValueAsString(snapshot));
            appointmentHistoryRepository.save(history);

            log.debug("Histórico salvo: appointmentId={}, action={}", appointment.getId(), action);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar snapshot do agendamento", e);
        }
    }

    private void publishEvent(Appointment appointment, String eventType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("appointmentId", appointment.getId().toString());
            payload.put("patientId", appointment.getPatient().getUserId().toString());
            payload.put("doctorId", appointment.getDoctor().getUserId().toString());
            payload.put("startAt", appointment.getStartAt().toString());
            payload.put("endAt", appointment.getEndAt().toString());
            payload.put("status", appointment.getStatus().toString());
            payload.put("eventType", eventType);
            payload.put("timestamp", OffsetDateTime.now().toString());

            OutboxEvent event = new OutboxEvent(
                    "Appointment",
                    appointment.getId().toString(),
                    eventType,
                    objectMapper.writeValueAsString(payload)
            );

            outboxEventRepository.save(event);
            log.debug("Evento publicado no outbox: appointmentId={}, eventType={}", appointment.getId(), eventType);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar payload do evento", e);
            throw new RuntimeException("Erro ao publicar evento", e);
        }
    }
}

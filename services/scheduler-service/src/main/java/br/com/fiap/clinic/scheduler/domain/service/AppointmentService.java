package br.com.fiap.clinic.scheduler.domain.service;

import br.com.fiap.clinic.scheduler.domain.entity.*;
import br.com.fiap.clinic.scheduler.domain.repository.*;
import br.com.fiap.clinic.scheduler.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final UserService userService;

    private final OutboxEventRepository outboxEventRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;

    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Appointment> findAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Appointment findById(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada com ID: " + id));
    }

    // --- CRIAÇÃO ---
    @Transactional
    public Appointment createAppointment(UUID patientId, UUID doctorId, UUID createdByUserId,
                                         OffsetDateTime startAt, OffsetDateTime endAt) {
        log.info("Criando agendamento: paciente={}, médico={}, início={}, fim={}",
                patientId, doctorId, startAt, endAt);

        validateDates(startAt, endAt);

        Patient patient = patientService.findById(patientId);
        if (!patient.getIsActive()) throw new IllegalArgumentException("Paciente inativo");

        Doctor doctor = doctorService.findById(doctorId);
        if (!doctor.getIsActive()) throw new IllegalArgumentException("Médico inativo");

        User creator = userService.findById(createdByUserId);

        List<Appointment> doctorConflicts = appointmentRepository.findDoctorConflictingAppointments(
                doctor.getId(),
                startAt,
                endAt
        );
        if (!doctorConflicts.isEmpty()) {
            throw new IllegalStateException("Médico já possui consulta agendada neste horário");
        }

        List<Appointment> patientConflicts = appointmentRepository.findPatientConflictingAppointments(
                patient.getId(),
                startAt,
                endAt
        );
        if (!patientConflicts.isEmpty()) {
            throw new IllegalStateException("Paciente já possui consulta agendada neste horário");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setCreatedBy(creator);
        appointment.setStartAt(startAt);
        appointment.setEndAt(endAt);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setActive(true);

        appointment = appointmentRepository.save(appointment);

        // 1. Salva Histórico (Log)
        saveHistory(appointment, "CREATED");
        // 2. Envia Kafka (Outbox)
        createOutboxEvent(appointment, "AppointmentCreated");

        log.info("Agendamento criado: {}", appointment.getId());
        return appointment;
    }

    // --- CONFIRMAÇÃO ---
    @Transactional
    public Appointment confirmAppointment(UUID id) {
        Appointment appointment = findById(id);

        // Validar se a consulta está ativa
        validateAppointmentActive(appointment);

        // Validar se a consulta ainda não iniciou
        validateNotStarted(appointment);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED &&
            appointment.getStatus() != AppointmentStatus.RESCHEDULED) {
            throw new IllegalStateException("Apenas consultas AGENDADAS ou REAGENDADAS podem ser confirmadas");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment = appointmentRepository.save(appointment);

        // 1. Salva Histórico (Log)
        saveHistory(appointment, "CONFIRMED");
        // 2. Envia Kafka (Outbox) - Notification vai mandar email de confirmação
        createOutboxEvent(appointment, "AppointmentConfirmed");

        return appointment;
    }

    // --- CANCELAMENTO ---
    @Transactional
    public Appointment cancelAppointment(UUID id) {
        Appointment appointment = findById(id);

        // Validar se a consulta está ativa
        validateAppointmentActive(appointment);

        // Validar prazo mínimo para cancelamento (24 horas)
        validateCancellationDeadline(appointment);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Não é possível cancelar consultas já realizadas ou canceladas");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment = appointmentRepository.save(appointment);

        // 1. Salva Histórico (Log)
        saveHistory(appointment, "CANCELLED");
        // 2. Envia Kafka (Outbox) - Notification pode mandar email de cancelamento
        createOutboxEvent(appointment, "AppointmentCancelled");

        return appointment;
    }

    // --- FINALIZAÇÃO ---
    @Transactional
    public Appointment completeAppointment(UUID id) {
        Appointment appointment = findById(id);

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Apenas consultas CONFIRMADAS podem ser finalizadas");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment = appointmentRepository.save(appointment);

        // 1. Salva Histórico (Log)
        saveHistory(appointment, "COMPLETED");
        // 2. Envia Kafka (Outbox)
        createOutboxEvent(appointment, "AppointmentCompleted");

        return appointment;
    }

    // --- REAGENDAMENTO (ATUALIZAÇÃO) ---
    @Transactional
    public Appointment rescheduleAppointment(UUID id, OffsetDateTime newStart, OffsetDateTime newEnd) {
        Appointment appointment = findById(id);

        // Validar se a consulta está ativa
        validateAppointmentActive(appointment);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Não é possível reagendar consultas realizadas ou canceladas");
        }

        validateDates(newStart, newEnd);

        List<Appointment> doctorConflicts = appointmentRepository.findDoctorConflictingAppointments(
                appointment.getDoctor().getId(),
                newStart,
                newEnd
        );
        // Remover a própria consulta da lista de conflitos
        doctorConflicts.removeIf(a -> a.getId().equals(id));
        if (!doctorConflicts.isEmpty()) {
            throw new IllegalStateException("Médico já possui consulta agendada no novo horário");
        }

        List<Appointment> patientConflicts = appointmentRepository.findPatientConflictingAppointments(
                appointment.getPatient().getId(),
                newStart,
                newEnd
        );
        // Remover a própria consulta da lista de conflitos
        patientConflicts.removeIf(a -> a.getId().equals(id));
        if (!patientConflicts.isEmpty()) {
            throw new IllegalStateException("Paciente já possui consulta agendada no novo horário");
        }

        appointment.setStartAt(newStart);
        appointment.setEndAt(newEnd);
        appointment.setStatus(AppointmentStatus.RESCHEDULED);

        appointment = appointmentRepository.save(appointment);

        // 1. Salva Histórico (Log)
        saveHistory(appointment, "RESCHEDULED");
        // 2. Envia Kafka (Outbox)
        createOutboxEvent(appointment, "AppointmentRescheduled");

        return appointment;
    }

    @Transactional(readOnly = true)
    public List<AppointmentHistory> findAppointmentHistory(UUID appointmentId) {
        return appointmentHistoryRepository.findByAppointmentIdOrderByEventTimeDesc(appointmentId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatusAndIsActiveTrue(status);
    }

    // --- MÉTODOS AUXILIARES ---

    private void validateDates(OffsetDateTime start, OffsetDateTime end) {
        // 1. Data fim deve ser após início
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new IllegalArgumentException("Data de início deve ser antes da data de fim");
        }

        // 2. Antecedência mínima de 1 hora
        OffsetDateTime minimumAllowedTime = OffsetDateTime.now().plusHours(1);
        if (start.isBefore(minimumAllowedTime)) {
            throw new IllegalArgumentException("Agendamento deve ser feito com pelo menos 1 hora de antecedência");
        }

        // 3. Validar horário comercial (8h às 18h)
        int startHour = start.getHour();
        int endHour = end.getHour();
        int endMinute = end.getMinute();

        if (startHour < 8 || startHour >= 18) {
            throw new IllegalArgumentException("Horário de início deve estar entre 8h e 18h");
        }

        // Permite terminar às 18h (18:00), mas não depois
        if (endHour > 18 || (endHour == 18 && endMinute > 0)) {
            throw new IllegalArgumentException("Horário de término deve ser até 18h");
        }

        // 4. Validar dias úteis (segunda a sexta)
        int dayOfWeek = start.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        if (dayOfWeek == 6 || dayOfWeek == 7) { // Sábado ou Domingo
            throw new IllegalArgumentException("Agendamentos são permitidos apenas em dias úteis (segunda a sexta)");
        }

        // 5. Validar duração da consulta (mínimo 15 min, máximo 4 horas)
        long durationMinutes = java.time.Duration.between(start, end).toMinutes();
        if (durationMinutes < 15) {
            throw new IllegalArgumentException("Duração mínima da consulta é 15 minutos");
        }
        if (durationMinutes > 240) { // 4 horas
            throw new IllegalArgumentException("Duração máxima da consulta é 4 horas");
        }
    }

    private void validateNotStarted(Appointment appointment) {
        OffsetDateTime now = OffsetDateTime.now();
        if (appointment.getStartAt().isBefore(now) || appointment.getStartAt().isEqual(now)) {
            throw new IllegalStateException("Não é possível confirmar consulta que já iniciou ou está em andamento");
        }
    }

    private void validateCancellationDeadline(Appointment appointment) {
        OffsetDateTime minimumCancellationTime = OffsetDateTime.now().plusHours(24);
        if (appointment.getStartAt().isBefore(minimumCancellationTime)) {
            throw new IllegalStateException("Cancelamento deve ser feito com pelo menos 24 horas de antecedência");
        }
    }

    private void validateAppointmentActive(Appointment appointment) {
        if (!appointment.isActive()) {
            throw new IllegalStateException("Operação não permitida: consulta está inativa");
        }
    }

    private void saveHistory(Appointment appointment, String action) {
        try {
            AppointmentHistory history = new AppointmentHistory();
            history.setAppointment(appointment);
            history.setAction(action);

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("status", appointment.getStatus());
            snapshot.put("startAt", appointment.getStartAt().toString());
            snapshot.put("endAt", appointment.getEndAt().toString());

            history.setSnapshot(objectMapper.writeValueAsString(snapshot));
            appointmentHistoryRepository.save(history);
        } catch (JsonProcessingException e) {
            log.error("Erro ao salvar histórico de auditoria", e);
        }
    }

    private void createOutboxEvent(Appointment appointment, String eventType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("appointmentId", appointment.getId().toString());
            payload.put("eventType", eventType);
            payload.put("timestamp", OffsetDateTime.now().toString());
            payload.put("status", appointment.getStatus().toString());

            // DADOS DO PACIENTE
            payload.put("patientId", appointment.getPatient().getId());
            payload.put("patientName", appointment.getPatient().getName());
            payload.put("patientEmail", appointment.getPatient().getEmail());

            // DADOS DO MÉDICO
            payload.put("doctorName", appointment.getDoctor().getName());
            payload.put("doctorSpecialty", appointment.getDoctor().getSpecialty());

            // DATAS
            payload.put("appointmentDate", appointment.getStartAt().toString());

            OutboxEvent event = new OutboxEvent();
            event.setAggregateType("Appointment");
            event.setAggregateId(appointment.getId().toString());
            event.setEventType(eventType);
            event.setPayload(objectMapper.writeValueAsString(payload));

            outboxEventRepository.save(event);
            log.info("Evento Outbox salvo com sucesso: {}", eventType);
        } catch (Exception e) {
            log.error("Erro CRÍTICO ao criar evento Outbox. O Kafka não receberá esta mensagem!", e);
            throw new RuntimeException("Erro ao gerar evento de integração", e);
        }
    }
}
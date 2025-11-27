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
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

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

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + patientId));
        if (!patient.getIsActive()) throw new IllegalArgumentException("Paciente inativo");

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado: " + doctorId));
        if (!doctor.getIsActive()) throw new IllegalArgumentException("Médico inativo");

        User creator = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário criador não encontrado: " + createdByUserId));

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setCreatedBy(creator);
        appointment.setStartAt(startAt);
        appointment.setEndAt(endAt);
        appointment.setStatus(AppointmentStatus.SOLICITADO);
        appointment.setActive(true);

        appointment = appointmentRepository.save(appointment);

        // 1. Salva Histórico (Log)
        saveHistory(appointment, "CRIADO");
        // 2. Envia Kafka (Outbox)
        createOutboxEvent(appointment, "AppointmentCreated");

        log.info("Agendamento criado: {}", appointment.getId());
        return appointment;
    }

    // --- CONFIRMAÇÃO ---
    @Transactional
    public Appointment confirmAppointment(UUID id) {
        Appointment appointment = findById(id);

        if (appointment.getStatus() != AppointmentStatus.SOLICITADO) {
            throw new IllegalStateException("Apenas consultas SOLICITADAS podem ser confirmadas");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMADO);
        appointment = appointmentRepository.save(appointment);

        // 1. Salva Histórico (Log)
        saveHistory(appointment, "CONFIRMADO");
        // 2. Envia Kafka (Outbox) - Notification vai mandar email de confirmação
        createOutboxEvent(appointment, "AppointmentConfirmed");

        return appointment;
    }

    // --- CANCELAMENTO ---
    @Transactional
    public Appointment cancelAppointment(UUID id) {
        Appointment appointment = findById(id);

        if (appointment.getStatus() == AppointmentStatus.REALIZADO ||
                appointment.getStatus() == AppointmentStatus.CANCELADO) {
            throw new IllegalStateException("Não é possível cancelar consultas já realizadas ou canceladas");
        }

        appointment.setStatus(AppointmentStatus.CANCELADO);
        appointment = appointmentRepository.save(appointment);

        // 1. Salva Histórico (Log)
        saveHistory(appointment, "CANCELADO");
        // 2. Envia Kafka (Outbox) - Notification pode mandar email de cancelamento
        createOutboxEvent(appointment, "AppointmentCancelled");

        return appointment;
    }

    // --- FINALIZAÇÃO ---
    @Transactional
    public Appointment completeAppointment(UUID id) {
        Appointment appointment = findById(id);

        if (appointment.getStatus() != AppointmentStatus.CONFIRMADO) {
            throw new IllegalStateException("Apenas consultas CONFIRMADAS podem ser finalizadas");
        }

        appointment.setStatus(AppointmentStatus.REALIZADO);
        appointment = appointmentRepository.save(appointment);

        // 1. Salva Histórico (Log)
        saveHistory(appointment, "REALIZADO");
        // 2. Envia Kafka (Outbox)
        createOutboxEvent(appointment, "AppointmentCompleted");

        return appointment;
    }

    // --- REAGENDAMENTO (ATUALIZAÇÃO) ---
    @Transactional
    public Appointment rescheduleAppointment(UUID id, OffsetDateTime newStart, OffsetDateTime newEnd) {
        Appointment appointment = findById(id);

        if (appointment.getStatus() == AppointmentStatus.REALIZADO ||
                appointment.getStatus() == AppointmentStatus.CANCELADO) {
            throw new IllegalStateException("Não é possível reagendar consultas realizadas ou canceladas");
        }

        validateDates(newStart, newEnd);

        appointment.setStartAt(newStart);
        appointment.setEndAt(newEnd);
        // Opcional: Voltar para SOLICITADO se a regra de negócio exigir nova aprovação
        // appointment.setStatus(AppointmentStatus.SOLICITADO);

        appointment = appointmentRepository.save(appointment);

        // 1. Salva Histórico (Log) - Isso responde sua pergunta: SIM, estamos salvando log na atualização!
        saveHistory(appointment, "REAGENDADO");
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
        if (start.isAfter(end)) throw new IllegalArgumentException("Início deve ser antes do fim");
        if (start.isBefore(OffsetDateTime.now())) throw new IllegalArgumentException("Data no passado não permitida");
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
            // Adicionar mais campos se necessário para auditoria

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
            // Atenção: Verifique se sua entidade Patient tem getName()/getEmail() herdados de User ou se precisa de .getUser().getName()
            // Assumindo que Patient estende User ou delega:
            payload.put("patientName", appointment.getPatient().getName());
            payload.put("patientEmail", appointment.getPatient().getEmail());

            // DADOS DO MÉDICO
            // IMPORTANTE: Aqui estava o erro! O consumer precisa do 'doctorSpecialty'.
            payload.put("doctorName", appointment.getDoctor().getName());
            payload.put("doctorSpecialty", appointment.getDoctor().getSpecialty()); // <--- CAMPO OBRIGATÓRIO PARA O NOTIFICATION

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
            // Em produção, lançar exceção para rollback
            throw new RuntimeException("Erro ao gerar evento de integração", e);
        }
    }
}
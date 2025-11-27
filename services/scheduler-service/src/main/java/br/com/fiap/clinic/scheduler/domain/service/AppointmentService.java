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

/**
 * Serviço de gerenciamento de consultas.
 * Responsável pelas operações de CRUD, regras de negócio e publicação de eventos (Outbox).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    
    // Repositórios para garantir a integridade e eventos
    private final OutboxEventRepository outboxEventRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    
    private final ObjectMapper objectMapper;

    /**
     * Busca todas as consultas.
     */
    @Transactional(readOnly = true)
    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    /**
     * Busca consultas com paginação.
     */
    @Transactional(readOnly = true)
    public Page<Appointment> findAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    /**
     * Busca uma consulta por ID.
     */
    @Transactional(readOnly = true)
    public Appointment findById(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada com ID: " + id));
    }

    /**
     * Cria uma nova consulta com validações e evento de criação.
     */
    @Transactional
    public Appointment createAppointment(UUID patientId, UUID doctorId, UUID createdByUserId,
                                         OffsetDateTime startAt, OffsetDateTime endAt) {
        log.info("Criando agendamento: paciente={}, médico={}, início={}, fim={}", 
                patientId, doctorId, startAt, endAt);

        // 1. Validações de Data (Lógica do seu colega)
        if (startAt.isAfter(endAt)) {
            throw new IllegalArgumentException("Data de início deve ser anterior à data de fim");
        }
        if (startAt.isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Não é possível agendar consultas no passado");
        }

        // 2. Busca e Validação de Entidades (Nossa lógica de conexão com repositórios)
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado: " + patientId));
        
        if (!patient.getIsActive()) { // Validação do colega
            throw new IllegalArgumentException("Paciente está inativo e não pode agendar consultas");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado: " + doctorId));
        
        if (!doctor.getIsActive()) { // Validação do colega
            throw new IllegalArgumentException("Médico está inativo e não pode receber consultas");
        }

        User creator = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário criador não encontrado: " + createdByUserId));

        // 3. Construção do Objeto
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setCreatedBy(creator);
        appointment.setStartAt(startAt);
        appointment.setEndAt(endAt);
        appointment.setStatus(AppointmentStatus.SOLICITADO); // Status inicial
        appointment.setActive(true);

        // 4. Persistência
        appointment = appointmentRepository.save(appointment);
        log.info("Agendamento criado com sucesso: id={}", appointment.getId());

        // 5. Auditoria e Eventos (Nossa lógica essencial para o Kafka)
        saveHistory(appointment, "CRIADO");
        createOutboxEvent(appointment, "AppointmentCreated");

        return appointment;
    }

    /**
     * Confirma uma consulta.
     */
    @Transactional
    public Appointment confirmAppointment(UUID id) {
        log.info("Confirmando agendamento: id={}", id);
        Appointment appointment = findById(id);

        // Regra de negócio do colega
        if (appointment.getStatus() != AppointmentStatus.SOLICITADO) {
            throw new IllegalStateException("Apenas consultas com status SOLICITADO podem ser confirmadas");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMADO);
        appointment = appointmentRepository.save(appointment);

        // Gera evento para notificar (ex: enviar email)
        saveHistory(appointment, "CONFIRMADO");
        createOutboxEvent(appointment, "AppointmentConfirmed");

        return appointment;
    }

    /**
     * Cancela uma consulta.
     */
    @Transactional
    public Appointment cancelAppointment(UUID id) {
        log.info("Cancelando agendamento: id={}", id);
        Appointment appointment = findById(id);

        // Regras de negócio do colega
        if (appointment.getStatus() == AppointmentStatus.REALIZADO) {
            throw new IllegalStateException("Não é possível cancelar consultas já realizadas");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELADO) {
            throw new IllegalStateException("Consulta já está cancelada");
        }

        appointment.setStatus(AppointmentStatus.CANCELADO);
        appointment = appointmentRepository.save(appointment);

        saveHistory(appointment, "CANCELADO");
        createOutboxEvent(appointment, "AppointmentCancelled");

        return appointment;
    }

    /**
     * Marca uma consulta como realizada.
     */
    @Transactional
    public Appointment completeAppointment(UUID id) {
        log.info("Concluindo agendamento: id={}", id);
        Appointment appointment = findById(id);

        if (appointment.getStatus() == AppointmentStatus.CANCELADO) {
            throw new IllegalStateException("Não é possível marcar como realizada uma consulta cancelada");
        }
        if (appointment.getStatus() == AppointmentStatus.REALIZADO) {
            throw new IllegalStateException("Consulta já está marcada como realizada");
        }

        appointment.setStatus(AppointmentStatus.REALIZADO);
        appointment = appointmentRepository.save(appointment);

        saveHistory(appointment, "REALIZADO");
        createOutboxEvent(appointment, "AppointmentCompleted");

        return appointment;
    }

    /**
     * Reagenda uma consulta (Atualiza datas).
     */
    @Transactional
    public Appointment rescheduleAppointment(UUID id, OffsetDateTime newStart, OffsetDateTime newEnd) {
        log.info("Reagendando: id={}, novo início={}, novo fim={}", id, newStart, newEnd);
        Appointment appointment = findById(id);

        if (appointment.getStatus() == AppointmentStatus.REALIZADO || 
            appointment.getStatus() == AppointmentStatus.CANCELADO) {
            throw new IllegalStateException("Não é possível reagendar consultas realizadas ou canceladas");
        }
        
        if (newStart.isAfter(newEnd)) {
            throw new IllegalArgumentException("Data de início deve ser anterior à data de fim");
        }

        appointment.setStartAt(newStart);
        appointment.setEndAt(newEnd);
        // Opcional: Voltar status para SOLICITADO se necessário, ou manter o atual
        
        appointment = appointmentRepository.save(appointment);

        saveHistory(appointment, "REAGENDADO");
        createOutboxEvent(appointment, "AppointmentRescheduled");

        return appointment;
    }
    
    /**
     * Busca histórico de alterações.
     */
    @Transactional(readOnly = true)
    public List<AppointmentHistory> findAppointmentHistory(UUID appointmentId) {
        return appointmentHistoryRepository.findByAppointmentIdOrderByEventTimeDesc(appointmentId);
    }
    
    // Lista consultas por Status
    @Transactional(readOnly = true)
    public List<Appointment> findByStatus(AppointmentStatus status) {
        return appointmentRepository.findByStatusAndIsActiveTrue(status);
    }

    // ==================== Métodos Auxiliares Privados ====================

    private void saveHistory(Appointment appointment, String action) {
        try {
            AppointmentHistory history = new AppointmentHistory();
            history.setAppointment(appointment);
            history.setAction(action);

            // Criar snapshot simples
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("status", appointment.getStatus());
            snapshot.put("startAt", appointment.getStartAt().toString());
            snapshot.put("endAt", appointment.getEndAt().toString());

            history.setSnapshot(objectMapper.writeValueAsString(snapshot));
            appointmentHistoryRepository.save(history);
        } catch (JsonProcessingException e) {
            log.error("Erro ao salvar histórico", e);
        }
    }

    private void createOutboxEvent(Appointment appointment, String eventType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("appointmentId", appointment.getId());
            payload.put("eventType", eventType);
            payload.put("timestamp", OffsetDateTime.now().toString());
            payload.put("status", appointment.getStatus().toString());
            
            // Dados enriquecidos para o consumidor (Notification Service)
            payload.put("patientId", appointment.getPatient().getId());
            payload.put("patientName", appointment.getPatient().getName());
            payload.put("patientEmail", appointment.getPatient().getEmail());
            payload.put("doctorName", appointment.getDoctor().getName());
            payload.put("appointmentDate", appointment.getStartAt().toString());

            OutboxEvent event = new OutboxEvent();
            event.setAggregateType("Appointment");
            event.setAggregateId(appointment.getId().toString());
            event.setEventType(eventType);
            event.setPayload(objectMapper.writeValueAsString(payload));

            outboxEventRepository.save(event);
            log.info("Evento Outbox salvo: {}", eventType);
        } catch (Exception e) {
            log.error("Erro ao criar evento Outbox", e);
            // Em produção, considere lançar a exceção para rollback da transação
        }
    }
}
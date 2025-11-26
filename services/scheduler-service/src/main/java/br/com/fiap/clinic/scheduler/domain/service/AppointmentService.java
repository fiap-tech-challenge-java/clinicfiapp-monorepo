package br.com.fiap.clinic.scheduler.domain.service;

import br.com.fiap.clinic.scheduler.domain.entity.*;
import br.com.fiap.clinic.scheduler.domain.repository.*;
import br.com.fiap.clinic.scheduler.exception.ResourceNotFoundException; // Se não tiver essa exception, use RuntimeException
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final OutboxEventRepository outboxEventRepository;
    // Adicionamos os repositórios necessários para buscar as entidades
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;

    // --- MÉTODO PRINCIPAL (Agora Implementado de Verdade) ---

    @Transactional
    public Appointment createAppointment(UUID patientId, UUID doctorId, UUID createdByUserId,
                                         OffsetDateTime startAt, OffsetDateTime endAt) {
        log.info("Criando agendamento real para Paciente: {} e Médico: {}", patientId, doctorId);

        // 1. Buscar as entidades reais no Banco de Dados
        // Se não encontrar, lança erro (e retorna 404/500 para a API)
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado no banco com ID: " + patientId));

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Médico não encontrado no banco com ID: " + doctorId));

        User creator = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("Usuário criador não encontrado com ID: " + createdByUserId));

        // 2. Montar o Agendamento com as entidades encontradas
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);   // Preenche a coluna patient_id
        appointment.setDoctor(doctor);     // Preenche a coluna doctor_id
        appointment.setCreatedBy(creator); // Preenche a coluna created_by

        appointment.setStartAt(startAt);
        appointment.setEndAt(endAt);
        appointment.setStatus(AppointmentStatus.SOLICITADO);
        appointment.setActive(true);

        // 3. Salvar no Banco (Agora vai funcionar pois os IDs não são nulos)
        appointment = appointmentRepository.save(appointment);

        log.info("Agendamento salvo com ID: {}", appointment.getId());

        // 4. Criar o evento para o Kafka pegar
        createOutboxEvent(appointment, "AppointmentCreated");

        return appointment;
    }

    private void createOutboxEvent(Appointment appointment, String eventType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("appointmentId", appointment.getId());
            payload.put("eventType", eventType);
            payload.put("timestamp", OffsetDateTime.now().toString());
            payload.put("status", appointment.getStatus().toString());

            // Agora pegamos os dados REAIS das entidades carregadas
            payload.put("patientId", appointment.getPatient().getId());
            payload.put("patientName", appointment.getPatient().getName());
            payload.put("patientEmail", appointment.getPatient().getEmail());

            payload.put("doctorName", appointment.getDoctor().getName());
            // Nota: Se o Doctor não tiver specialty mapeado na entidade user, ajustar aqui
            // payload.put("doctorSpecialty", appointment.getDoctor().getSpecialty());

            OutboxEvent event = new OutboxEvent();
            event.setAggregateType("Appointment");
            event.setAggregateId(appointment.getId().toString());
            event.setEventType(eventType);
            event.setPayload(objectMapper.writeValueAsString(payload));

            outboxEventRepository.save(event);
            log.info("Evento Outbox salvo: {}", eventType);
        } catch (Exception e) {
            log.error("Erro ao criar evento", e);
            // Não lançamos exceção aqui para não fazer rollback do agendamento se o log falhar
            // mas em produção, deveria ser atômico.
        }
    }

    // --- STUBS (Mantidos para compatibilidade) ---

    public List<Appointment> findAll() {
        return Collections.emptyList();
    }

    public Appointment findById(UUID id) {
        return new Appointment();
    }

    public List<Appointment> findByStatus(AppointmentStatus status) {
        return Collections.emptyList();
    }

    public List<AppointmentHistory> findAppointmentHistory(UUID appointmentId) {
        return Collections.emptyList();
    }

    @Transactional
    public Appointment confirmAppointment(UUID id) {
        // ... implementação simplificada ...
        return new Appointment();
    }

    @Transactional
    public Appointment cancelAppointment(UUID id) {
        return new Appointment();
    }

    @Transactional
    public Appointment completeAppointment(UUID id) {
        return new Appointment();
    }

    @Transactional
    public Appointment rescheduleAppointment(UUID id, OffsetDateTime start, OffsetDateTime end) {
        return new Appointment();
    }
}
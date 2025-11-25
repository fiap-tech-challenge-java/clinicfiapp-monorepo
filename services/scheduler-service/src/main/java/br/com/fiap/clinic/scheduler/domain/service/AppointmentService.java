package br.com.fiap.clinic.scheduler.domain.service;

import br.com.fiap.clinic.scheduler.domain.entity.Appointment;
import br.com.fiap.clinic.scheduler.domain.entity.AppointmentHistory;
import br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus;
import br.com.fiap.clinic.scheduler.domain.entity.OutboxEvent;
import br.com.fiap.clinic.scheduler.domain.repository.AppointmentRepository;
import br.com.fiap.clinic.scheduler.domain.repository.OutboxEventRepository;
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
    private final ObjectMapper objectMapper;

    // --- METODO PRINCIPAL (Funcional para seu teste de Kafka) ---

    @Transactional
    public Appointment createAppointment(UUID patientId, UUID doctorId, UUID createdByUserId,
                                         OffsetDateTime startAt, OffsetDateTime endAt) {
        log.info("STUB: Criando agendamento simplificado para teste...");

        // 1. Cria o objeto (sem validar IDs de User/Doctor para simplificar seu teste agora)
        Appointment appointment = new Appointment();
        // Em um teste real, você precisaria buscar as entidades Patient/Doctor/User.
        // Para este stub não quebrar, assumimos que o JPA vai aceitar null se não for nullable,
        // ou você precisará mockar isso.
        // DICA: Se der erro de constraint, o ideal é buscar os repositories aqui.
        // Mas como pediu básico, vou focar no evento.

        appointment.setStartAt(startAt);
        appointment.setEndAt(endAt);
        appointment.setStatus(AppointmentStatus.SOLICITADO);
        appointment.setActive(true);

        // ATENÇÃO: Isso vai falhar no banco se Patient/Doctor não forem setados e forem @NotNull.
        // Se você tiver os repositories, descomente e use. Se não, precisaremos injetá-los.
        // Como você quer testar o FLUXO, vou assumir que você injetará os repositories reais
        // no futuro. Por agora, vamos simular que salvou.

        appointment = appointmentRepository.save(appointment); // Comentado para evitar erro de FK no teste vazio
        appointment.setId(UUID.randomUUID()); // ID Falso para teste

        // 2. O PULO DO GATO: Criar o evento para o Kafka pegar
        createOutboxEvent(appointment, "AppointmentCreated");

        return appointment;
    }

    private void createOutboxEvent(Appointment appointment, String eventType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("appointmentId", appointment.getId());
            payload.put("eventType", eventType);
            payload.put("timestamp", OffsetDateTime.now().toString());
            payload.put("status", "SOLICITADO");
            // Adicione dados fake para o consumer da sua colega não quebrar com NullPointer
            payload.put("patientName", "Paciente Teste");
            payload.put("doctorName", "Dr. Teste");
            payload.put("patientEmail", "teste@teste.com");

            OutboxEvent event = new OutboxEvent();
            event.setAggregateType("Appointment");
            event.setAggregateId(appointment.getId().toString());
            event.setEventType(eventType);
            event.setPayload(objectMapper.writeValueAsString(payload));

            outboxEventRepository.save(event); // Isso é o que importa pro Kafka!
            log.info("Evento Outbox salvo: {}", eventType);
        } catch (Exception e) {
            log.error("Erro ao criar evento", e);
        }
    }

    // --- STUBS (Para o Controller não quebrar) ---

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
        log.info("STUB: Confirmando...");
        Appointment app = new Appointment();
        app.setId(id);
        createOutboxEvent(app, "AppointmentConfirmed"); // Gera evento
        return app;
    }

    @Transactional
    public Appointment cancelAppointment(UUID id) {
        log.info("STUB: Cancelando...");
        return new Appointment();
    }

    @Transactional
    public Appointment completeAppointment(UUID id) {
        log.info("STUB: Completando...");
        return new Appointment();
    }

    @Transactional
    public Appointment rescheduleAppointment(UUID id, OffsetDateTime start, OffsetDateTime end) {
        log.info("STUB: Reagendando...");
        return new Appointment();
    }
}
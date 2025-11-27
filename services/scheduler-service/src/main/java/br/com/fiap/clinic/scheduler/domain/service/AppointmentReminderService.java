package br.com.fiap.clinic.scheduler.domain.service;

import br.com.fiap.clinic.scheduler.domain.entity.Appointment;
import br.com.fiap.clinic.scheduler.domain.entity.OutboxEvent;
import br.com.fiap.clinic.scheduler.domain.repository.AppointmentRepository;
import br.com.fiap.clinic.scheduler.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderService {

    private final AppointmentRepository appointmentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Envia lembretes para todas as consultas do próximo dia
     */
    @Transactional
    public void sendDailyReminders() {
        log.info("Iniciando envio de lembretes diários de consultas");

        // Define o período para buscar consultas (próximas 24 horas)
        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.with(LocalTime.MAX);

        // Busca todas as consultas do dia seguinte
        List<Appointment> appointments = appointmentRepository.findAppointmentsForReminder(
            tomorrow,
            endOfTomorrow
        );

        log.info("Encontradas {} consultas para enviar lembretes", appointments.size());

        // Para cada consulta, cria um evento no outbox
        for (Appointment appointment : appointments) {
            try {
                createReminderEvent(appointment);
                log.debug("Lembrete criado para consulta ID: {}", appointment.getId());
            } catch (Exception e) {
                log.error("Erro ao criar lembrete para consulta ID: {}", appointment.getId(), e);
            }
        }

        log.info("Processamento de lembretes diários concluído");
    }

    /**
     * Cria um evento de lembrete no outbox para ser processado pelo relay
     */
    private void createReminderEvent(Appointment appointment) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("appointmentId", appointment.getId().toString());
            payload.put("patientId", appointment.getPatient().getId().toString());
            payload.put("patientName", appointment.getPatient().getName());
            payload.put("patientEmail", appointment.getPatient().getEmail());
            payload.put("doctorName", appointment.getDoctor().getName());
            payload.put("doctorSpecialty", appointment.getDoctor().getSpecialty());
            payload.put("appointmentDate", appointment.getStartAt().toString());
            payload.put("appointmentTime", appointment.getStartAt().toLocalTime().toString());
            payload.put("notificationType", "APPOINTMENT_REMINDER");

            OutboxEvent event = new OutboxEvent();
            event.setAggregateType("Appointment");
            event.setAggregateId(appointment.getId().toString());
            event.setEventType("AppointmentReminderRequested");
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setProcessed(false);
            event.setCreatedAt(LocalDateTime.now());

            outboxEventRepository.save(event);

        } catch (Exception e) {
            log.error("Erro ao criar evento de lembrete", e);
            throw new RuntimeException("Falha ao criar evento de lembrete", e);
        }
    }
}


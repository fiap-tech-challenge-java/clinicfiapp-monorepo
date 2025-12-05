package br.com.fiap.clinic.notification.domain.service;

import br.com.fiap.clinic.notification.domain.entity.Notification;
import br.com.fiap.clinic.notification.domain.enums.Channel;
import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import br.com.fiap.clinic.notification.domain.enums.Status;
import br.com.fiap.clinic.notification.domain.repository.NotificationRepository;
import br.com.fiap.clinic.notification.domain.dto.AppointmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class NotificationHandlerService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    public NotificationHandlerService(NotificationRepository notificationRepository, EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    /**
     * Processa a confirmação de agendamento e cria uma notificação
     * não reenvia se já foi processado
     */
    @Transactional
    public void handleAppointmentConfirmation(AppointmentEvent appointmentEvent) {
        log.info("Processando confirmação de agendamento - Appointment ID: {}, Patient: {}",
                appointmentEvent.appointmentId(), appointmentEvent.patientName());

        Optional<Notification> existingNotification = notificationRepository
                .findByAppointmentIdAndNotificationTypeAndChannel(
                        appointmentEvent.appointmentId(),
                        NotificationType.APPOINTMENT,
                        Channel.EMAIL
                );

        if (existingNotification.isPresent()) {
            Notification existing = existingNotification.get();
            if (existing.getStatus() == Status.SENT) {
                log.warn("IDEMPOTÊNCIA: Notificação já foi enviada anteriormente. " +
                        "Appointment ID: {}, Status: {}, Sent At: {}",
                        appointmentEvent.appointmentId(), existing.getStatus(), existing.getSentAt());
                return; // Não reenvia
            } else if (existing.getStatus() == Status.FAILED && existing.getAttempts() >= MAX_RETRY_ATTEMPTS) {
                log.error("IDEMPOTÊNCIA: Notificação falhou após {} tentativas. Não tentando novamente.",
                        existing.getAttempts());
                return; // Já tentou demais
            }
            // Se FAILED com tentativas < MAX, tenta reenviar
            log.info("Tentando reenviar notificação falhada. Tentativa: {}", existing.getAttempts() + 1);
            processNotification(existing, appointmentEvent, NotificationType.APPOINTMENT);
            return;
        }

        // Cria nova notificação
        Notification notificationEntity = new Notification(
                appointmentEvent.appointmentId(),
                appointmentEvent.patientId(),
                NotificationType.APPOINTMENT,
                Channel.EMAIL,
                Status.PENDING,
                LocalDateTime.now()
        );
        notificationEntity.setEventId(generateEventId(appointmentEvent));

        try {
            notificationRepository.save(notificationEntity);
            processNotification(notificationEntity, appointmentEvent, NotificationType.APPOINTMENT);
        } catch (DataIntegrityViolationException e) {
            // Tratamento de race condition - outro thread já criou a notificação
            log.warn("IDEMPOTÊNCIA: Constraint violation ao salvar notificação. " +
                    "Provavelmente já processada por outro consumidor. Appointment ID: {}",
                    appointmentEvent.appointmentId());
        }
    }

    /**
     * Processa o lembrete de consulta e cria uma notificação
     * não reenvia se já foi processado
     */
    @Transactional
    public void handleAppointmentReminder(AppointmentEvent appointmentEvent) {
        log.info("Processando lembrete de consulta - Appointment ID: {}, Patient: {}",
                appointmentEvent.appointmentId(), appointmentEvent.patientName());

        // Verifica se já existe uma notificação processada
        Optional<Notification> existingNotification = notificationRepository
                .findByAppointmentIdAndNotificationTypeAndChannel(
                        appointmentEvent.appointmentId(),
                        NotificationType.APPOINTMENT_REMINDER,
                        Channel.EMAIL
                );

        if (existingNotification.isPresent()) {
            Notification existing = existingNotification.get();
            if (existing.getStatus() == Status.SENT) {
                log.warn("IDEMPOTÊNCIA: Lembrete já foi enviado anteriormente. " +
                        "Appointment ID: {}, Status: {}, Sent At: {}",
                        appointmentEvent.appointmentId(), existing.getStatus(), existing.getSentAt());
                return; // Não reenvia
            } else if (existing.getStatus() == Status.FAILED && existing.getAttempts() >= MAX_RETRY_ATTEMPTS) {
                log.error("IDEMPOTÊNCIA: Lembrete falhou após {} tentativas. Não tentando novamente.",
                        existing.getAttempts());
                return; // Já tentou demais
            }
            // Se FAILED com tentativas < MAX, tenta reenviar
            log.info("Tentando reenviar lembrete falhado. Tentativa: {}", existing.getAttempts() + 1);
            processNotification(existing, appointmentEvent, NotificationType.APPOINTMENT_REMINDER);
            return;
        }

        // Cria nova notificação
        Notification notificationEntity = new Notification(
                appointmentEvent.appointmentId(),
                appointmentEvent.patientId(),
                NotificationType.APPOINTMENT_REMINDER,
                Channel.EMAIL,
                Status.PENDING,
                LocalDateTime.now()
        );
        notificationEntity.setEventId(generateEventId(appointmentEvent));

        try {
            notificationRepository.save(notificationEntity);
            processNotification(notificationEntity, appointmentEvent, NotificationType.APPOINTMENT_REMINDER);
        } catch (DataIntegrityViolationException e) {
            // Tratamento de race condition
            log.warn("IDEMPOTÊNCIA: Constraint violation ao salvar lembrete. " +
                    "Provavelmente já processado por outro consumidor. Appointment ID: {}",
                    appointmentEvent.appointmentId());
        }
    }

    /**
     * Processa o envio da notificação com tratamento de erro e resiliência
     */
    private void processNotification(Notification notification, AppointmentEvent event, NotificationType type) {
        LocalDateTime appointmentDate = LocalDateTime.parse(
                event.appointmentDate(),
                java.time.format.DateTimeFormatter.ISO_DATE_TIME
        );

        try {
            // Envia o email com template HTML
            emailService.sendEmail(
                    event.patientEmail(),
                    type,
                    event.patientName(),
                    event.doctorName(),
                    event.doctorSpecialty(),
                    appointmentDate
            );

            // Sucesso - atualiza o status
            notification.setStatus(Status.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setLastError(null); // Limpa erro anterior se houver
            notificationRepository.save(notification);

            log.info("✅ Notificação enviada com sucesso para {} - Tipo: {}",
                    event.patientEmail(), type);

        } catch (Exception e) {
            // Registra erro e incrementa tentativas
            notification.setStatus(Status.FAILED);
            notification.setAttempts(notification.getAttempts() + 1);
            notification.setLastError(truncateError(e.getMessage()));
            notificationRepository.save(notification);

            log.error("❌ ERRO ao enviar notificação para {} - Tipo: {} - Tentativa {}/{} - Erro: {}",
                    event.patientEmail(), type, notification.getAttempts(), MAX_RETRY_ATTEMPTS, e.getMessage());

            // Se atingiu o máximo de tentativas, lança exceção para DLT
            if (notification.getAttempts() >= MAX_RETRY_ATTEMPTS) {
                log.error("🚨 Máximo de tentativas atingido. Mensagem será enviada para DLT.");
                throw new RuntimeException("Falha após " + MAX_RETRY_ATTEMPTS + " tentativas: " + e.getMessage(), e);
            }

            // Propaga exceção para retry do Kafka
            throw new RuntimeException("Erro ao processar notificação. Será reprocessada.", e);
        }
    }

    /**
     * Gera um ID único para o evento baseado nos dados
     */
    private String generateEventId(AppointmentEvent event) {
        return event.appointmentId() + "-" + event.patientId() + "-" + System.currentTimeMillis();
    }

    /**
     * Trunca mensagem de erro para não estourar tamanho da coluna
     */
    private String truncateError(String error) {
        if (error == null) return null;
        return error.length() > 500 ? error.substring(0, 500) : error;
    }
}

package br.com.fiap.clinic.notification.domain.service;

import br.com.fiap.clinic.notification.domain.entity.Notification;
import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import br.com.fiap.clinic.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationHandlerService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    /**
     * Processa o lembrete de consulta e cria uma notificação
     */
    @Transactional
    public void handleAppointmentReminder(
            UUID appointmentId,
            UUID patientId,
            String patientName,
            String patientEmail,
            String doctorName,
            String doctorSpecialty,
            LocalDateTime appointmentDate
    ) {
        log.info("Processando lembrete de consulta - Appointment ID: {}, Patient: {}",
                appointmentId, patientName);

        // Cria a notificação
        Notification notification = new Notification();
        notification.setAppointmentId(appointmentId);
        notification.setPatientId(patientId);
        notification.setNotificationType(NotificationType.LEMBRETE);
        notification.setChannel("EMAIL");
        notification.setStatus("PENDING");
        notification.setAttempts(0);
        notification.setScheduledFor(LocalDateTime.now());
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);

        try {
            // Envia o email com template HTML bonitinho
            emailService.sendEmail(
                    patientEmail,
                    NotificationType.LEMBRETE,
                    patientName,
                    doctorName,
                    doctorSpecialty,
                    appointmentDate
            );

            // Atualiza o status após o envio
            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
            log.info("Lembrete enviado com sucesso para {}", patientEmail);

        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification.setAttempts(notification.getAttempts() + 1);
            log.error("Erro ao enviar lembrete para {}", patientEmail, e);
        }

        notificationRepository.save(notification);
    }

    /**
     * Processa a confirmação de agendamento e cria uma notificação
     */
    @Transactional
    public void handleAppointmentConfirmation(
            UUID appointmentId,
            UUID patientId,
            String patientName,
            String patientEmail,
            String doctorName,
            String doctorSpecialty,
            LocalDateTime appointmentDate
    ) {
        log.info("Processando confirmação de agendamento - Appointment ID: {}, Patient: {}",
                appointmentId, patientName);

        // Cria a notificação
        Notification notification = new Notification();
        notification.setAppointmentId(appointmentId);
        notification.setPatientId(patientId);
        notification.setNotificationType(NotificationType.AGENDAMENTO);
        notification.setChannel("EMAIL");
        notification.setStatus("PENDING");
        notification.setAttempts(0);
        notification.setScheduledFor(LocalDateTime.now());
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);

        try {
            // Envia o email com template HTML bonitinho
            emailService.sendEmail(
                    patientEmail,
                    NotificationType.AGENDAMENTO,
                    patientName,
                    doctorName,
                    doctorSpecialty,
                    appointmentDate
            );

            // Atualiza o status após o envio
            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
            log.info("Confirmação de agendamento enviada com sucesso para {}", patientEmail);

        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification.setAttempts(notification.getAttempts() + 1);
            log.error("Erro ao enviar confirmação para {}", patientEmail, e);
        }

        notificationRepository.save(notification);
    }
}

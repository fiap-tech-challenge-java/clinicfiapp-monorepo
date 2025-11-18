package br.com.fiap.clinic.notification.domain.service;

import br.com.fiap.clinic.notification.domain.entity.NotificationEntity;
import br.com.fiap.clinic.notification.domain.enums.Channel;
import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import br.com.fiap.clinic.notification.domain.enums.Status;
import br.com.fiap.clinic.notification.domain.repository.NotificationRepository;
import br.com.fiap.clinic.notification.dto.AppointmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class NotificationHandlerService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationHandlerService(NotificationRepository notificationRepository, EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    /**
     * Processa a confirmação de agendamento e cria uma notificação
     */
    @Transactional
    public void handleAppointmentConfirmation(AppointmentEvent appointmentEvent) {
        log.info("Processando confirmação de agendamento - Appointment ID: {}, Patient: {}",
                appointmentEvent.appointmentId(), appointmentEvent.patientName());

        // Cria a notificação
        NotificationEntity notificationEntity = new NotificationEntity(
                appointmentEvent.appointmentId(),
                appointmentEvent.patientId(),
                NotificationType.AGENDAMENTO,
                Channel.EMAIL,
                Status.PENDING,
                LocalDateTime.now()
        );

        notificationRepository.save(notificationEntity);

        try {
            // Envia o email com template HTML bonitinho
            emailService.sendEmail(
                    appointmentEvent.patientEmail(),
                    NotificationType.AGENDAMENTO,
                    appointmentEvent.patientName(),
                    appointmentEvent.doctorName(),
                    appointmentEvent.doctorSpecialty(),
                    appointmentEvent.appointmentDate()
            );

            // Atualiza o status após o envio
            notificationEntity.setStatus(Status.SENT);
            notificationEntity.setSentAt(LocalDateTime.now());
            log.info("Confirmação de agendamento enviada com sucesso para {}", appointmentEvent.patientEmail());

        } catch (Exception e) {
            notificationEntity.setStatus(Status.FAILED);
            notificationEntity.setAttempts(notificationEntity.getAttempts() + 1);
            log.error("Erro ao enviar confirmação para {}", appointmentEvent.patientEmail(), e);
        }

        notificationRepository.save(notificationEntity);
    }

    /**
     * Processa o lembrete de consulta e cria uma notificação
     */
    @Transactional
    public void handleAppointmentReminder(AppointmentEvent appointmentEvent) {
        log.info("Processando lembrete de consulta - Appointment ID: {}, Patient: {}",
                appointmentEvent.appointmentId(), appointmentEvent.patientName());

        // Cria a notificação
        NotificationEntity notificationEntity = new NotificationEntity(
                appointmentEvent.appointmentId(),
                appointmentEvent.patientId(),
                NotificationType.LEMBRETE,
                Channel.EMAIL,
                Status.PENDING,
                LocalDateTime.now()
        );

        notificationRepository.save(notificationEntity);

        try {
            // Envia o email com template HTML bonitinho
            emailService.sendEmail(
                    appointmentEvent.patientEmail(),
                    NotificationType.LEMBRETE,
                    appointmentEvent.patientName(),
                    appointmentEvent.doctorName(),
                    appointmentEvent.doctorSpecialty(),
                    appointmentEvent.appointmentDate()
            );

            // Atualiza o status após o envio
            notificationEntity.setStatus(Status.SENT);
            notificationEntity.setSentAt(LocalDateTime.now());
            log.info("Lembrete enviado com sucesso para {}", appointmentEvent.patientEmail());

        } catch (Exception e) {
            notificationEntity.setStatus(Status.FAILED);
            notificationEntity.setAttempts(notificationEntity.getAttempts() + 1);
            log.error("Erro ao enviar lembrete para {}", appointmentEvent.patientEmail(), e);
        }

        notificationRepository.save(notificationEntity);
    }

}

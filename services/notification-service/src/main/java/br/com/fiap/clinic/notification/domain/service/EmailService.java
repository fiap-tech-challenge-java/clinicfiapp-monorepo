package br.com.fiap.clinic.notification.domain.service;

import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    /**
     * Envia um email com template HTML
     */
    public void sendEmail(
            String to,
            NotificationType type,
            String patientName,
            String doctorName,
            String doctorSpecialty,
            LocalDateTime appointmentDate
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(templateService.getSubject(type));

            String htmlContent = templateService.buildEmailTemplate(
                    type,
                    patientName,
                    doctorName,
                    doctorSpecialty,
                    appointmentDate
            );

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email enviado com sucesso para: {} - Tipo: {}", to, type);

        } catch (MessagingException e) {
            log.error("Erro ao enviar email para: {}", to, e);
            throw new RuntimeException("Falha ao enviar email", e);
        }
    }
}


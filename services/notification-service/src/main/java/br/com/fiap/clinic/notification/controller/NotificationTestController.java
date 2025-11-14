package br.com.fiap.clinic.notification.controller;

import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import br.com.fiap.clinic.notification.domain.service.EmailTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/notifications/test")
public class NotificationTestController {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @PostMapping("/send-appointment-confirmation")
    public ResponseEntity<String> sendAppointmentConfirmation(@RequestParam String to) {
        try {
            // Dados de exemplo
            String patientName = "João Silva";
            String doctorName = "Dr. Maria Santos";
            String doctorSpecialty = "Cardiologia";
            LocalDateTime appointmentDate = LocalDateTime.now().plusDays(3).withHour(14).withMinute(30);

            // Gera o HTML do template
            String htmlContent = emailTemplateService.buildEmailTemplate(
                NotificationType.AGENDAMENTO,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
            );

            // Cria o email HTML
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("lucasfariascontato@gmail.com");
            helper.setTo(to);
            helper.setSubject("✅ Confirmação de Agendamento - ClinicFIAPP");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

            String dateFormatted = appointmentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            return ResponseEntity.ok(
                "✅ Email de confirmação de agendamento enviado com sucesso!\n\n" +
                "📋 Dados do teste:\n" +
                "Paciente: " + patientName + "\n" +
                "Médico: " + doctorName + "\n" +
                "Especialidade: " + doctorSpecialty + "\n" +
                "Data/Hora: " + dateFormatted + "\n\n" +
                "📧 Verifique sua caixa de entrada (e spam): " + to
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Erro: " + e.getMessage());
        }
    }

    @PostMapping("/send-appointment-reminder")
    public ResponseEntity<String> sendAppointmentReminder(@RequestParam String to) {
        try {
            // Dados de exemplo
            String patientName = "Maria Oliveira";
            String doctorName = "Dr. Pedro Costa";
            String doctorSpecialty = "Dermatologia";
            LocalDateTime appointmentDate = LocalDateTime.now().plusHours(2);

            // Gera o HTML do template
            String htmlContent = emailTemplateService.buildEmailTemplate(
                NotificationType.LEMBRETE,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
            );

            // Cria o email HTML
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("lucasfariascontato@gmail.com");
            helper.setTo(to);
            helper.setSubject("🔔 Lembrete: Sua consulta é hoje! - ClinicFIAPP");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

            String dateFormatted = appointmentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            return ResponseEntity.ok(
                "🔔 Email de lembrete enviado com sucesso!\n\n" +
                "📋 Dados do teste:\n" +
                "Paciente: " + patientName + "\n" +
                "Médico: " + doctorName + "\n" +
                "Especialidade: " + doctorSpecialty + "\n" +
                "Data/Hora: " + dateFormatted + "\n\n" +
                "📧 Verifique sua caixa de entrada (e spam): " + to
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Erro: " + e.getMessage());
        }
    }
}

package br.com.fiap.clinic.notification.unit.service;

import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import br.com.fiap.clinic.notification.domain.service.EmailService;
import br.com.fiap.clinic.notification.domain.service.EmailTemplateService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - EmailService")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailTemplateService templateService;

    @InjectMocks
    private EmailService emailService;

    @Mock
    private MimeMessage mimeMessage;

    private String recipientEmail;
    private String patientName;
    private String doctorName;
    private String doctorSpecialty;
    private LocalDateTime appointmentDate;

    @BeforeEach
    void setUp() {
        recipientEmail = "patient@test.com";
        patientName = "João Silva";
        doctorName = "Dr. Carlos Souza";
        doctorSpecialty = "Cardiologia";
        appointmentDate = LocalDateTime.now().plusDays(7);
    }

    // ==================== TESTES DE ENVIO DE EMAIL ====================

    @Test
    @DisplayName("Deve enviar email de confirmação de consulta com sucesso")
    void deveEnviarEmailConfirmacaoComSucesso() throws MessagingException {
        // Arrange
        String htmlContent = "<html><body>Confirmação de consulta</body></html>";
        String subject = "Confirmação de Consulta";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.getSubject(NotificationType.APPOINTMENT)).thenReturn(subject);
        when(templateService.buildEmailTemplate(
                eq(NotificationType.APPOINTMENT),
                eq(patientName),
                eq(doctorName),
                eq(doctorSpecialty),
                eq(appointmentDate)
        )).thenReturn(htmlContent);

        // Act
        emailService.sendEmail(
                recipientEmail,
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        verify(mailSender).createMimeMessage();
        verify(templateService).getSubject(NotificationType.APPOINTMENT);
        verify(templateService).buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Deve enviar email de lembrete de consulta com sucesso")
    void deveEnviarEmailLembreteComSucesso() throws MessagingException {
        // Arrange
        String htmlContent = "<html><body>Lembrete de consulta</body></html>";
        String subject = "Lembrete de Consulta";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.getSubject(NotificationType.APPOINTMENT_REMINDER)).thenReturn(subject);
        when(templateService.buildEmailTemplate(
                eq(NotificationType.APPOINTMENT_REMINDER),
                eq(patientName),
                eq(doctorName),
                eq(doctorSpecialty),
                eq(appointmentDate)
        )).thenReturn(htmlContent);

        // Act
        emailService.sendEmail(
                recipientEmail,
                NotificationType.APPOINTMENT_REMINDER,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        verify(mailSender).createMimeMessage();
        verify(templateService).getSubject(NotificationType.APPOINTMENT_REMINDER);
        verify(templateService).buildEmailTemplate(
                NotificationType.APPOINTMENT_REMINDER,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando ocorre erro ao criar mensagem")
    void deveLancarExcecaoQuandoErroAoCriarMensagem() {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.getSubject(any())).thenReturn("Subject");
        when(templateService.buildEmailTemplate(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn("<html><body>Test</body></html>");

        // Simula exceção ao enviar
        doThrow(new RuntimeException("SMTP connection failed"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThatThrownBy(() -> emailService.sendEmail(
                recipientEmail,
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SMTP connection failed");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando falha ao enviar email")
    void deveLancarExcecaoQuandoFalhaEnviarEmail() {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.getSubject(any())).thenReturn("Subject");
        when(templateService.buildEmailTemplate(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn("<html><body>Test</body></html>");

        doThrow(new RuntimeException("Mail server unavailable"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThatThrownBy(() -> emailService.sendEmail(
                recipientEmail,
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mail server unavailable");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Deve enviar email com encoding UTF-8")
    void deveEnviarEmailComEncodingUTF8() throws MessagingException {
        // Arrange
        String htmlContent = "<html><body>Conteúdo com acentuação</body></html>";
        String subject = "Confirmação de Consulta";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.getSubject(NotificationType.APPOINTMENT)).thenReturn(subject);
        when(templateService.buildEmailTemplate(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn(htmlContent);

        // Act
        emailService.sendEmail(
                recipientEmail,
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Deve construir email com dados corretos do paciente")
    void deveConstruirEmailComDadosCorretosPaciente() throws MessagingException {
        // Arrange
        String expectedSubject = "Confirmação de Consulta - Clínica FIAP";
        String htmlContent = "<html><body>Email para João Silva</body></html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.getSubject(NotificationType.APPOINTMENT)).thenReturn(expectedSubject);
        when(templateService.buildEmailTemplate(
                eq(NotificationType.APPOINTMENT),
                eq(patientName),
                eq(doctorName),
                eq(doctorSpecialty),
                eq(appointmentDate)
        )).thenReturn(htmlContent);

        // Act
        emailService.sendEmail(
                recipientEmail,
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        verify(templateService).buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );
        verify(templateService).getSubject(NotificationType.APPOINTMENT);
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Deve construir email com dados corretos do médico")
    void deveConstruirEmailComDadosCorretosMedico() throws MessagingException {
        // Arrange
        String htmlContent = "<html><body>Dr. Carlos Souza - Cardiologia</body></html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.getSubject(any())).thenReturn("Subject");
        when(templateService.buildEmailTemplate(
                any(),
                anyString(),
                eq(doctorName),
                eq(doctorSpecialty),
                any()
        )).thenReturn(htmlContent);

        // Act
        emailService.sendEmail(
                recipientEmail,
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        verify(templateService).buildEmailTemplate(
                any(),
                anyString(),
                eq(doctorName),
                eq(doctorSpecialty),
                any()
        );
    }

    @Test
    @DisplayName("Deve tratar diferentes tipos de notificação")
    void deveTratarDiferentesTiposNotificacao() throws MessagingException {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.getSubject(any())).thenReturn("Subject");
        when(templateService.buildEmailTemplate(any(), anyString(), anyString(), anyString(), any()))
                .thenReturn("<html><body>Test</body></html>");

        // Act - Confirmação
        emailService.sendEmail(
                recipientEmail,
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Act - Lembrete
        emailService.sendEmail(
                recipientEmail,
                NotificationType.APPOINTMENT_REMINDER,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        verify(templateService).buildEmailTemplate(
                eq(NotificationType.APPOINTMENT),
                anyString(), anyString(), anyString(), any()
        );
        verify(templateService).buildEmailTemplate(
                eq(NotificationType.APPOINTMENT_REMINDER),
                anyString(), anyString(), anyString(), any()
        );
        verify(mailSender, times(2)).send(mimeMessage);
    }

    @Test
    @DisplayName("Deve enviar email mesmo com data de consulta no passado")
    void deveEnviarEmailMesmoComDataPassado() throws MessagingException {
        // Arrange
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.getSubject(any())).thenReturn("Subject");
        when(templateService.buildEmailTemplate(any(), anyString(), anyString(), anyString(), eq(pastDate)))
                .thenReturn("<html><body>Test</body></html>");

        // Act
        emailService.sendEmail(
                recipientEmail,
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                pastDate
        );

        // Assert
        verify(templateService).buildEmailTemplate(
                any(), anyString(), anyString(), anyString(), eq(pastDate)
        );
        verify(mailSender).send(mimeMessage);
    }
}


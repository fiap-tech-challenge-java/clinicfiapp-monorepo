package br.com.fiap.clinic.notification.unit.service;

import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import br.com.fiap.clinic.notification.domain.service.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Testes Unitários - EmailTemplateService")
class EmailTemplateServiceTest {

    private EmailTemplateService emailTemplateService;

    private String patientName;
    private String doctorName;
    private String doctorSpecialty;
    private LocalDateTime appointmentDate;

    @BeforeEach
    void setUp() {
        emailTemplateService = new EmailTemplateService();
        patientName = "João Silva";
        doctorName = "Dr. Carlos Souza";
        doctorSpecialty = "Cardiologia";
        appointmentDate = LocalDateTime.of(2025, 12, 20, 14, 30);
    }

    // ==================== TESTES DE APPOINTMENT CONFIRMATION ====================

    @Test
    @DisplayName("Deve construir template de confirmação de consulta com todos os dados")
    void deveConstruirTemplateConfirmacaoComTodosDados() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).isNotNull();
        assertThat(template).isNotEmpty();
        assertThat(template).contains(patientName);
        assertThat(template).contains(doctorName);
        assertThat(template).contains(doctorSpecialty);
        assertThat(template).contains("20/12/2025"); // Data formatada
        assertThat(template).contains("14:30"); // Hora formatada
    }

    @Test
    @DisplayName("Deve incluir saudação com nome do paciente no template de confirmação")
    void deveIncluirSaudacaoComNomePacienteConfirmacao() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).contains("João Silva");
        assertThat(template).contains("Parabéns");
    }

    @Test
    @DisplayName("Deve incluir informações do médico no template de confirmação")
    void deveIncluirInformacoesMedicoConfirmacao() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).contains("Dr. Carlos Souza");
        assertThat(template).contains("Cardiologia");
    }

    @Test
    @DisplayName("Deve formatar data corretamente no template de confirmação")
    void deveFormatarDataCorretamenteConfirmacao() {
        // Arrange
        LocalDateTime specificDate = LocalDateTime.of(2025, 1, 15, 10, 0);

        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                specificDate
        );

        // Assert
        assertThat(template).contains("15/01/2025");
        assertThat(template).contains("10:00");
    }

    @Test
    @DisplayName("Deve construir template HTML válido para confirmação")
    void deveConstruirTemplateHTMLValidoConfirmacao() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).startsWith("<!DOCTYPE html>");
        assertThat(template).contains("<html");
        assertThat(template).contains("<head>");
        assertThat(template).contains("<body>");
        assertThat(template).contains("</body>");
        assertThat(template).contains("</html>");
    }

    @Test
    @DisplayName("Deve incluir CSS inline no template de confirmação")
    void deveIncluirCSSInlineConfirmacao() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).contains("<style>");
        assertThat(template).contains("</style>");
        assertThat(template).contains("background-color");
        assertThat(template).contains("font-family");
    }

    @Test
    @DisplayName("Deve incluir emoji de sucesso no template de confirmação")
    void deveIncluirEmojiSucessoConfirmacao() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).contains("✅");
    }

    // ==================== TESTES DE APPOINTMENT REMINDER ====================

    @Test
    @DisplayName("Deve construir template de lembrete com todos os dados")
    void deveConstruirTemplateLembreteComTodosDados() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT_REMINDER,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).isNotNull();
        assertThat(template).isNotEmpty();
        assertThat(template).contains(patientName);
        assertThat(template).contains(doctorName);
        assertThat(template).contains(doctorSpecialty);
        assertThat(template).contains("20/12/2025");
        assertThat(template).contains("14:30");
    }

    @Test
    @DisplayName("Deve incluir saudação com nome do paciente no template de lembrete")
    void deveIncluirSaudacaoComNomePacienteLembrete() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT_REMINDER,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).contains("João Silva");
        assertThat(template).containsIgnoringCase("Olá");
    }

    @Test
    @DisplayName("Deve incluir emoji de lembrete no template")
    void deveIncluirEmojiLembrete() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT_REMINDER,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).contains("🔔");
    }

    @Test
    @DisplayName("Deve construir template HTML válido para lembrete")
    void deveConstruirTemplateHTMLValidoLembrete() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT_REMINDER,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).startsWith("<!DOCTYPE html>");
        assertThat(template).contains("<html");
        assertThat(template).contains("<body>");
        assertThat(template).contains("</body>");
        assertThat(template).contains("</html>");
    }

    @Test
    @DisplayName("Deve formatar data corretamente no template de lembrete")
    void deveFormatarDataCorretamenteLembrete() {
        // Arrange
        LocalDateTime specificDate = LocalDateTime.of(2025, 3, 25, 16, 45);

        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT_REMINDER,
                patientName,
                doctorName,
                doctorSpecialty,
                specificDate
        );

        // Assert
        assertThat(template).contains("25/03/2025");
        assertThat(template).contains("16:45");
    }

    @Test
    @DisplayName("Deve incluir dicas no template de lembrete")
    void deveIncluirDicasLembrete() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT_REMINDER,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).contains("Lembre-se de");
        assertThat(template).containsIgnoringCase("documentos");
    }

    // ==================== TESTES DE SUBJECT ====================

    @Test
    @DisplayName("Deve retornar subject correto para confirmação de consulta")
    void deveRetornarSubjectCorretoParaConfirmacao() {
        // Act
        String subject = emailTemplateService.getSubject(NotificationType.APPOINTMENT);

        // Assert
        assertThat(subject).isNotNull();
        assertThat(subject).isNotEmpty();
        assertThat(subject).contains("✅");
        assertThat(subject).containsIgnoringCase("consulta");
        assertThat(subject).containsIgnoringCase("agendada");
    }

    @Test
    @DisplayName("Deve retornar subject correto para lembrete de consulta")
    void deveRetornarSubjectCorretoParaLembrete() {
        // Act
        String subject = emailTemplateService.getSubject(NotificationType.APPOINTMENT_REMINDER);

        // Assert
        assertThat(subject).isNotNull();
        assertThat(subject).isNotEmpty();
        assertThat(subject).contains("🔔");
        assertThat(subject).containsIgnoringCase("lembrete");
        assertThat(subject).containsIgnoringCase("consulta");
    }

    @Test
    @DisplayName("Deve retornar subjects diferentes para tipos diferentes")
    void deveRetornarSubjectsDiferentesParaTiposDiferentes() {
        // Act
        String subjectConfirmation = emailTemplateService.getSubject(NotificationType.APPOINTMENT);
        String subjectReminder = emailTemplateService.getSubject(NotificationType.APPOINTMENT_REMINDER);

        // Assert
        assertThat(subjectConfirmation).isNotEqualTo(subjectReminder);
    }

    // ==================== TESTES DE EDGE CASES ====================

    @Test
    @DisplayName("Deve tratar nomes com caracteres especiais")
    void deveTratarNomesComCaracteresEspeciais() {
        // Arrange
        String specialPatientName = "José da Silva Júnior";
        String specialDoctorName = "Dra. María González";

        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                specialPatientName,
                specialDoctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).contains("José da Silva Júnior");
        assertThat(template).contains("Dra. María González");
    }

    @Test
    @DisplayName("Deve tratar especialidade com acentuação")
    void deveTratarEspecialidadeComAcentuacao() {
        // Arrange
        String specialtyWithAccent = "Ortopedia e Traumatologia";

        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                specialtyWithAccent,
                appointmentDate
        );

        // Assert
        assertThat(template).contains("Ortopedia e Traumatologia");
    }

    @Test
    @DisplayName("Deve formatar horários com zero à esquerda")
    void deveFormatarHorariosComZeroEsquerda() {
        // Arrange
        LocalDateTime earlyMorning = LocalDateTime.of(2025, 6, 10, 8, 5);

        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                earlyMorning
        );

        // Assert
        assertThat(template).contains("08:05");
        assertThat(template).contains("10/06/2025");
    }

    @Test
    @DisplayName("Deve construir templates diferentes para cada tipo de notificação")
    void deveConstruirTemplatesDiferentesParaCadaTipo() {
        // Act
        String confirmationTemplate = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        String reminderTemplate = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT_REMINDER,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(confirmationTemplate).isNotEqualTo(reminderTemplate);
        assertThat(confirmationTemplate).contains("✅");
        assertThat(reminderTemplate).contains("🔔");
    }

    @Test
    @DisplayName("Deve incluir encoding UTF-8 no template")
    void deveIncluirEncodingUTF8() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).contains("UTF-8");
        assertThat(template).contains("charset");
    }

    @Test
    @DisplayName("Deve incluir viewport para responsividade")
    void deveIncluirViewportParaResponsividade() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).contains("viewport");
        assertThat(template).contains("width=device-width");
    }

    @Test
    @DisplayName("Deve incluir informação de não responder no footer")
    void deveIncluirInformacaoNaoResponder() {
        // Act
        String template = emailTemplateService.buildEmailTemplate(
                NotificationType.APPOINTMENT,
                patientName,
                doctorName,
                doctorSpecialty,
                appointmentDate
        );

        // Assert
        assertThat(template).containsIgnoringCase("não responda");
        assertThat(template).containsIgnoringCase("automático");
    }
}


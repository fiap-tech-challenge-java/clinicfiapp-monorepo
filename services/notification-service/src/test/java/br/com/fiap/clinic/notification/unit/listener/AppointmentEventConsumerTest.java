package br.com.fiap.clinic.notification.unit.listener;

import br.com.fiap.clinic.notification.domain.dto.AppointmentEvent;
import br.com.fiap.clinic.notification.domain.service.NotificationHandlerService;
import br.com.fiap.clinic.notification.listener.AppointmentEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - AppointmentEventConsumer")
class AppointmentEventConsumerTest {

    @Mock
    private NotificationHandlerService notificationHandlerService;

    @InjectMocks
    private AppointmentEventConsumer appointmentEventConsumer;

    private AppointmentEvent appointmentEvent;
    private int partition;
    private long offset;

    @BeforeEach
    void setUp() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        String doctorId = UUID.randomUUID().toString();

        appointmentEvent = new AppointmentEvent(
                appointmentId,
                patientId,
                "Maria Santos",
                "maria.santos@test.com",
                doctorId,
                "Dr. Roberto Lima",
                "Ortopedia",
                LocalDateTime.now().plusDays(5).toString()
        );

        partition = 0;
        offset = 12345L;
    }

    // ==================== TESTES DE CONSUMO DE EVENTOS ====================

    @Test
    @DisplayName("Deve processar evento de agendamento com sucesso")
    void deveProcessarEventoAgendamentoComSucesso() {
        // Arrange
        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(appointmentEvent);

        // Act
        appointmentEventConsumer.handler(appointmentEvent, partition, offset);

        // Assert
        verify(notificationHandlerService).handleAppointmentConfirmation(appointmentEvent);
    }

    @Test
    @DisplayName("Deve processar evento com informações corretas do paciente")
    void deveProcessarEventoComInformacoesCorretas() {
        // Arrange
        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(any(AppointmentEvent.class));

        // Act
        appointmentEventConsumer.handler(appointmentEvent, partition, offset);

        // Assert
        verify(notificationHandlerService).handleAppointmentConfirmation(argThat(event ->
                event.patientName().equals("Maria Santos") &&
                event.patientEmail().equals("maria.santos@test.com") &&
                event.doctorName().equals("Dr. Roberto Lima") &&
                event.doctorSpecialty().equals("Ortopedia")
        ));
    }

    @Test
    @DisplayName("Deve propagar exceção quando falha ao processar evento")
    void devePropagaExcecaoQuandoFalhaProcessar() {
        // Arrange
        RuntimeException exception = new RuntimeException("Erro ao processar evento");
        doThrow(exception).when(notificationHandlerService).handleAppointmentConfirmation(appointmentEvent);

        // Act & Assert
        assertThatThrownBy(() -> appointmentEventConsumer.handler(appointmentEvent, partition, offset))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro ao processar evento");

        verify(notificationHandlerService).handleAppointmentConfirmation(appointmentEvent);
    }

    @Test
    @DisplayName("Deve processar evento de diferentes partições")
    void deveProcessarEventoDiferentesParticoes() {
        // Arrange
        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(appointmentEvent);

        // Act - Partição 0
        appointmentEventConsumer.handler(appointmentEvent, 0, 100L);

        // Act - Partição 1
        appointmentEventConsumer.handler(appointmentEvent, 1, 200L);

        // Act - Partição 2
        appointmentEventConsumer.handler(appointmentEvent, 2, 300L);

        // Assert
        verify(notificationHandlerService, times(3)).handleAppointmentConfirmation(appointmentEvent);
    }

    @Test
    @DisplayName("Deve processar múltiplos eventos sequencialmente")
    void deveProcessarMultiplosEventosSequencialmente() {
        // Arrange
        AppointmentEvent event1 = new AppointmentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Paciente 1",
                "paciente1@test.com",
                UUID.randomUUID().toString(),
                "Dr. A",
                "Cardiologia",
                LocalDateTime.now().plusDays(1).toString()
        );

        AppointmentEvent event2 = new AppointmentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Paciente 2",
                "paciente2@test.com",
                UUID.randomUUID().toString(),
                "Dr. B",
                "Dermatologia",
                LocalDateTime.now().plusDays(2).toString()
        );

        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(any(AppointmentEvent.class));

        // Act
        appointmentEventConsumer.handler(event1, 0, 1L);
        appointmentEventConsumer.handler(event2, 0, 2L);

        // Assert
        verify(notificationHandlerService).handleAppointmentConfirmation(event1);
        verify(notificationHandlerService).handleAppointmentConfirmation(event2);
        verify(notificationHandlerService, times(2)).handleAppointmentConfirmation(any(AppointmentEvent.class));
    }

    @Test
    @DisplayName("Deve processar evento com appointmentId válido")
    void deveProcessarEventoComAppointmentIdValido() {
        // Arrange
        UUID expectedAppointmentId = appointmentEvent.appointmentId();
        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(any(AppointmentEvent.class));

        // Act
        appointmentEventConsumer.handler(appointmentEvent, partition, offset);

        // Assert
        verify(notificationHandlerService).handleAppointmentConfirmation(argThat(event ->
                event.appointmentId().equals(expectedAppointmentId)
        ));
    }

    @Test
    @DisplayName("Deve processar evento com patientId válido")
    void deveProcessarEventoComPatientIdValido() {
        // Arrange
        UUID expectedPatientId = appointmentEvent.patientId();
        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(any(AppointmentEvent.class));

        // Act
        appointmentEventConsumer.handler(appointmentEvent, partition, offset);

        // Assert
        verify(notificationHandlerService).handleAppointmentConfirmation(argThat(event ->
                event.patientId().equals(expectedPatientId)
        ));
    }

    @Test
    @DisplayName("Deve processar evento com data de consulta válida")
    void deveProcessarEventoComDataConsultaValida() {
        // Arrange
        String expectedDate = appointmentEvent.appointmentDate();
        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(any(AppointmentEvent.class));

        // Act
        appointmentEventConsumer.handler(appointmentEvent, partition, offset);

        // Assert
        verify(notificationHandlerService).handleAppointmentConfirmation(argThat(event ->
                event.appointmentDate().equals(expectedDate)
        ));
    }

    @Test
    @DisplayName("Deve lançar exceção quando serviço de notificação falha")
    void deveLancarExcecaoQuandoServicoFalha() {
        // Arrange
        doThrow(new RuntimeException("Serviço indisponível"))
                .when(notificationHandlerService).handleAppointmentConfirmation(appointmentEvent);

        // Act & Assert
        assertThatThrownBy(() -> appointmentEventConsumer.handler(appointmentEvent, partition, offset))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Serviço indisponível");

        verify(notificationHandlerService).handleAppointmentConfirmation(appointmentEvent);
    }

    @Test
    @DisplayName("Deve processar evento com offset crescente")
    void deveProcessarEventoComOffsetCrescente() {
        // Arrange
        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(appointmentEvent);

        // Act
        appointmentEventConsumer.handler(appointmentEvent, 0, 100L);
        appointmentEventConsumer.handler(appointmentEvent, 0, 101L);
        appointmentEventConsumer.handler(appointmentEvent, 0, 102L);

        // Assert
        verify(notificationHandlerService, times(3)).handleAppointmentConfirmation(appointmentEvent);
    }

    @Test
    @DisplayName("Deve processar evento com informações completas do médico")
    void deveProcessarEventoComInformacoesCompletasMedico() {
        // Arrange
        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(any(AppointmentEvent.class));

        // Act
        appointmentEventConsumer.handler(appointmentEvent, partition, offset);

        // Assert
        verify(notificationHandlerService).handleAppointmentConfirmation(argThat(event ->
                event.doctorId() != null &&
                !event.doctorId().isEmpty() &&
                event.doctorName().equals("Dr. Roberto Lima") &&
                event.doctorSpecialty().equals("Ortopedia")
        ));
    }

    @Test
    @DisplayName("Deve processar evento mesmo quando falha e lançar exceção para retry")
    void deveProcessarEventoMesmoQuandoFalhaELancarExcecao() {
        // Arrange
        RuntimeException expectedError = new RuntimeException("Timeout ao enviar email");
        doThrow(expectedError).when(notificationHandlerService).handleAppointmentConfirmation(appointmentEvent);

        // Act & Assert
        assertThatThrownBy(() -> appointmentEventConsumer.handler(appointmentEvent, partition, offset))
                .isEqualTo(expectedError);

        verify(notificationHandlerService).handleAppointmentConfirmation(appointmentEvent);
    }

    @Test
    @DisplayName("Deve invocar handleAppointmentConfirmation apenas uma vez por evento")
    void deveInvocarHandleApenasUmaVezPorEvento() {
        // Arrange
        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(appointmentEvent);

        // Act
        appointmentEventConsumer.handler(appointmentEvent, partition, offset);

        // Assert
        verify(notificationHandlerService, times(1)).handleAppointmentConfirmation(appointmentEvent);
        verifyNoMoreInteractions(notificationHandlerService);
    }

    @Test
    @DisplayName("Deve processar evento com email válido do paciente")
    void deveProcessarEventoComEmailValidoPaciente() {
        // Arrange
        String expectedEmail = "maria.santos@test.com";
        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(any(AppointmentEvent.class));

        // Act
        appointmentEventConsumer.handler(appointmentEvent, partition, offset);

        // Assert
        verify(notificationHandlerService).handleAppointmentConfirmation(argThat(event ->
                event.patientEmail().equals(expectedEmail) &&
                event.patientEmail().contains("@")
        ));
    }

    @Test
    @DisplayName("Deve processar evento com data no formato ISO")
    void deveProcessarEventoComDataFormatoISO() {
        // Arrange
        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(any(AppointmentEvent.class));

        // Act
        appointmentEventConsumer.handler(appointmentEvent, partition, offset);

        // Assert
        verify(notificationHandlerService).handleAppointmentConfirmation(argThat(event ->
                event.appointmentDate() != null &&
                !event.appointmentDate().isEmpty()
        ));
    }

    // ==================== TESTES DE EDGE CASES ====================

    @Test
    @DisplayName("Deve processar evento com nome de paciente com caracteres especiais")
    void deveProcessarEventoComNomeCaracteresEspeciais() {
        // Arrange
        AppointmentEvent specialEvent = new AppointmentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "José da Silva Júnior",
                "jose.junior@test.com",
                UUID.randomUUID().toString(),
                "Dra. María González",
                "Pediatria",
                LocalDateTime.now().plusDays(3).toString()
        );

        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(any(AppointmentEvent.class));

        // Act
        appointmentEventConsumer.handler(specialEvent, partition, offset);

        // Assert
        verify(notificationHandlerService).handleAppointmentConfirmation(argThat(event ->
                event.patientName().contains("José") &&
                event.patientName().contains("Júnior") &&
                event.doctorName().contains("María")
        ));
    }

    @Test
    @DisplayName("Deve processar evento com especialidade composta")
    void deveProcessarEventoComEspecialidadeComposta() {
        // Arrange
        AppointmentEvent specialtyEvent = new AppointmentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Paciente Teste",
                "paciente@test.com",
                UUID.randomUUID().toString(),
                "Dr. Teste",
                "Ortopedia e Traumatologia",
                LocalDateTime.now().plusDays(1).toString()
        );

        doNothing().when(notificationHandlerService).handleAppointmentConfirmation(any(AppointmentEvent.class));

        // Act
        appointmentEventConsumer.handler(specialtyEvent, partition, offset);

        // Assert
        verify(notificationHandlerService).handleAppointmentConfirmation(argThat(event ->
                event.doctorSpecialty().equals("Ortopedia e Traumatologia")
        ));
    }
}


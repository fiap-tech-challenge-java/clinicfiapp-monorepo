package br.com.fiap.clinic.notification.unit.service;

import br.com.fiap.clinic.notification.domain.dto.AppointmentEvent;
import br.com.fiap.clinic.notification.domain.entity.Notification;
import br.com.fiap.clinic.notification.domain.enums.Channel;
import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import br.com.fiap.clinic.notification.domain.enums.Status;
import br.com.fiap.clinic.notification.domain.repository.NotificationRepository;
import br.com.fiap.clinic.notification.domain.service.EmailService;
import br.com.fiap.clinic.notification.domain.service.NotificationHandlerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - NotificationHandlerService")
class NotificationHandlerServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationHandlerService notificationHandlerService;

    private AppointmentEvent appointmentEvent;
    private Notification notification;
    private UUID appointmentId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
        patientId = UUID.randomUUID();

        appointmentEvent = new AppointmentEvent(
                appointmentId,
                patientId,
                "João Silva",
                "joao.silva@test.com",
                UUID.randomUUID().toString(),
                "Dr. Carlos Souza",
                "Cardiologia",
                LocalDateTime.now().plusDays(7).toString()
        );

        notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setAppointmentId(appointmentId);
        notification.setPatientId(patientId);
        notification.setNotificationType(NotificationType.APPOINTMENT);
        notification.setChannel(Channel.EMAIL);
        notification.setStatus(Status.PENDING);
        notification.setAttempts(0);
        notification.setScheduledFor(LocalDateTime.now());
        notification.setCreatedAt(LocalDateTime.now());
    }

    // ==================== TESTES DE APPOINTMENT CONFIRMATION ====================

    @Test
    @DisplayName("Deve criar e enviar notificação de confirmação com sucesso")
    void deveCriarEEnviarNotificacaoConfirmacaoComSucesso() {
        // Arrange
        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT, Channel.EMAIL
        )).thenReturn(Optional.empty());

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        doNothing().when(emailService).sendEmail(
                anyString(), any(), anyString(), anyString(), anyString(), any()
        );

        // Act
        notificationHandlerService.handleAppointmentConfirmation(appointmentEvent);

        // Assert
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        // Both captors reference the same notification instance that gets updated
        // So we check the final state after both saves
        Notification savedNotification = captor.getAllValues().get(1);
        assertThat(savedNotification.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(savedNotification.getNotificationType()).isEqualTo(NotificationType.APPOINTMENT);
        assertThat(savedNotification.getStatus()).isEqualTo(Status.SENT);
        assertThat(savedNotification.getSentAt()).isNotNull();

        verify(emailService).sendEmail(
                eq(appointmentEvent.patientEmail()),
                eq(NotificationType.APPOINTMENT),
                eq(appointmentEvent.patientName()),
                eq(appointmentEvent.doctorName()),
                eq(appointmentEvent.doctorSpecialty()),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("Deve ignorar notificação já enviada anteriormente (idempotência)")
    void deveIgnorarNotificacaoJaEnviada() {
        // Arrange
        notification.setStatus(Status.SENT);
        notification.setSentAt(LocalDateTime.now().minusHours(1));

        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT, Channel.EMAIL
        )).thenReturn(Optional.of(notification));

        // Act
        notificationHandlerService.handleAppointmentConfirmation(appointmentEvent);

        // Assert
        verify(emailService, never()).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve reenviar notificação falhada com tentativas < MAX")
    void deveReenviarNotificacaoFalhada() {
        // Arrange
        notification.setStatus(Status.FAILED);
        notification.setAttempts(1);
        notification.setLastError("Erro anterior");

        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT, Channel.EMAIL
        )).thenReturn(Optional.of(notification));

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(emailService).sendEmail(
                anyString(), any(), anyString(), anyString(), anyString(), any()
        );

        // Act
        notificationHandlerService.handleAppointmentConfirmation(appointmentEvent);

        // Assert
        verify(emailService).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(Status.SENT);
        assertThat(saved.getLastError()).isNull();
    }

    @Test
    @DisplayName("Deve ignorar notificação falhada após máximo de tentativas")
    void deveIgnorarNotificacaoAposMaximoTentativas() {
        // Arrange
        notification.setStatus(Status.FAILED);
        notification.setAttempts(3);

        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT, Channel.EMAIL
        )).thenReturn(Optional.of(notification));

        // Act
        notificationHandlerService.handleAppointmentConfirmation(appointmentEvent);

        // Assert
        verify(emailService, never()).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve registrar falha ao enviar email e incrementar tentativas")
    void deveRegistrarFalhaAoEnviarEmail() {
        // Arrange
        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT, Channel.EMAIL
        )).thenReturn(Optional.empty());

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        doThrow(new RuntimeException("Falha ao enviar email"))
                .when(emailService).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());

        // Act & Assert
        assertThatThrownBy(() -> notificationHandlerService.handleAppointmentConfirmation(appointmentEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro ao processar notificação");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        Notification failedNotification = captor.getAllValues().get(1);
        assertThat(failedNotification.getStatus()).isEqualTo(Status.FAILED);
        assertThat(failedNotification.getAttempts()).isEqualTo(1);
        assertThat(failedNotification.getLastError()).isNotNull();
    }

    @Test
    @DisplayName("Deve lançar exceção após máximo de tentativas de falha")
    void deveLancarExcecaoAposMaximoTentativas() {
        // Arrange
        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT, Channel.EMAIL
        )).thenReturn(Optional.empty());

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        // Simula 3 falhas consecutivas
        doThrow(new RuntimeException("Falha persistente"))
                .when(emailService).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());

        // Act & Assert - Primeira tentativa
        assertThatThrownBy(() -> notificationHandlerService.handleAppointmentConfirmation(appointmentEvent))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeast(2)).save(captor.capture());

        Notification failedNotification = captor.getAllValues().get(1);
        assertThat(failedNotification.getStatus()).isEqualTo(Status.FAILED);
    }

    @Test
    @DisplayName("Deve tratar DataIntegrityViolationException por race condition")
    void deveTratarRaceCondition() {
        // Arrange
        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT, Channel.EMAIL
        )).thenReturn(Optional.empty());

        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        // Act
        notificationHandlerService.handleAppointmentConfirmation(appointmentEvent);

        // Assert
        verify(notificationRepository).save(any(Notification.class));
        verify(emailService, never()).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());
    }

    // ==================== TESTES DE APPOINTMENT REMINDER ====================

    @Test
    @DisplayName("Deve criar e enviar lembrete de consulta com sucesso")
    void deveCriarEEnviarLembreteComSucesso() {
        // Arrange
        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT_REMINDER, Channel.EMAIL
        )).thenReturn(Optional.empty());

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        doNothing().when(emailService).sendEmail(
                anyString(), any(), anyString(), anyString(), anyString(), any()
        );

        // Act
        notificationHandlerService.handleAppointmentReminder(appointmentEvent);

        // Assert
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        Notification firstSave = captor.getAllValues().get(0);
        assertThat(firstSave.getNotificationType()).isEqualTo(NotificationType.APPOINTMENT_REMINDER);

        Notification finalSave = captor.getAllValues().get(1);
        assertThat(finalSave.getStatus()).isEqualTo(Status.SENT);

        verify(emailService).sendEmail(
                eq(appointmentEvent.patientEmail()),
                eq(NotificationType.APPOINTMENT_REMINDER),
                eq(appointmentEvent.patientName()),
                eq(appointmentEvent.doctorName()),
                eq(appointmentEvent.doctorSpecialty()),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("Deve ignorar lembrete já enviado anteriormente (idempotência)")
    void deveIgnorarLembreteJaEnviado() {
        // Arrange
        notification.setNotificationType(NotificationType.APPOINTMENT_REMINDER);
        notification.setStatus(Status.SENT);
        notification.setSentAt(LocalDateTime.now().minusHours(1));

        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT_REMINDER, Channel.EMAIL
        )).thenReturn(Optional.of(notification));

        // Act
        notificationHandlerService.handleAppointmentReminder(appointmentEvent);

        // Assert
        verify(emailService, never()).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve reenviar lembrete falhado com tentativas < MAX")
    void deveReenviarLembreteFalhado() {
        // Arrange
        notification.setNotificationType(NotificationType.APPOINTMENT_REMINDER);
        notification.setStatus(Status.FAILED);
        notification.setAttempts(2);

        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT_REMINDER, Channel.EMAIL
        )).thenReturn(Optional.of(notification));

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(emailService).sendEmail(
                anyString(), any(), anyString(), anyString(), anyString(), any()
        );

        // Act
        notificationHandlerService.handleAppointmentReminder(appointmentEvent);

        // Assert
        verify(emailService).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(Status.SENT);
        assertThat(saved.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve ignorar lembrete falhado após máximo de tentativas")
    void deveIgnorarLembreteAposMaximoTentativas() {
        // Arrange
        notification.setNotificationType(NotificationType.APPOINTMENT_REMINDER);
        notification.setStatus(Status.FAILED);
        notification.setAttempts(3);

        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT_REMINDER, Channel.EMAIL
        )).thenReturn(Optional.of(notification));

        // Act
        notificationHandlerService.handleAppointmentReminder(appointmentEvent);

        // Assert
        verify(emailService, never()).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve registrar falha ao enviar lembrete e incrementar tentativas")
    void deveRegistrarFalhaAoEnviarLembrete() {
        // Arrange
        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT_REMINDER, Channel.EMAIL
        )).thenReturn(Optional.empty());

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        doThrow(new RuntimeException("Erro de conexão"))
                .when(emailService).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());

        // Act & Assert
        assertThatThrownBy(() -> notificationHandlerService.handleAppointmentReminder(appointmentEvent))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        Notification failedNotification = captor.getAllValues().get(1);
        assertThat(failedNotification.getStatus()).isEqualTo(Status.FAILED);
        assertThat(failedNotification.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve tratar DataIntegrityViolationException ao salvar lembrete")
    void deveTratarRaceConditionLembrete() {
        // Arrange
        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT_REMINDER, Channel.EMAIL
        )).thenReturn(Optional.empty());

        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint"));

        // Act
        notificationHandlerService.handleAppointmentReminder(appointmentEvent);

        // Assert
        verify(notificationRepository).save(any(Notification.class));
        verify(emailService, never()).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Deve truncar mensagem de erro longa")
    void deveTruncarMensagemErroLonga() {
        // Arrange
        String longError = "E".repeat(600); // Erro maior que 500 caracteres

        when(notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId, NotificationType.APPOINTMENT, Channel.EMAIL
        )).thenReturn(Optional.empty());

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        doThrow(new RuntimeException(longError))
                .when(emailService).sendEmail(anyString(), any(), anyString(), anyString(), anyString(), any());

        // Act & Assert
        assertThatThrownBy(() -> notificationHandlerService.handleAppointmentConfirmation(appointmentEvent))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeast(2)).save(captor.capture());

        Notification failedNotification = captor.getAllValues().get(1);
        assertThat(failedNotification.getLastError()).hasSize(500);
    }
}


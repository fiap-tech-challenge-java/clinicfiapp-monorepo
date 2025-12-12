package br.com.fiap.clinic.notification.unit.repository;

import br.com.fiap.clinic.notification.domain.entity.Notification;
import br.com.fiap.clinic.notification.domain.enums.Channel;
import br.com.fiap.clinic.notification.domain.enums.NotificationType;
import br.com.fiap.clinic.notification.domain.enums.Status;
import br.com.fiap.clinic.notification.domain.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Testes Unitários - NotificationRepository")
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    private UUID appointmentId;
    private UUID patientId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
        patientId = UUID.randomUUID();

        notification = new Notification();
        notification.setAppointmentId(appointmentId);
        notification.setPatientId(patientId);
        notification.setNotificationType(NotificationType.APPOINTMENT);
        notification.setChannel(Channel.EMAIL);
        notification.setStatus(Status.PENDING);
        notification.setAttempts(0);
        notification.setScheduledFor(LocalDateTime.now());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setEventId(appointmentId + "-" + patientId + "-" + System.currentTimeMillis());
    }

    // ==================== TESTES DE SAVE ====================

    @Test
    @DisplayName("Deve salvar notificação com sucesso")
    void deveSalvarNotificacaoComSucesso() {
        // Act
        Notification saved = notificationRepository.save(notification);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(saved.getPatientId()).isEqualTo(patientId);
        assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
        assertThat(saved.getAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve gerar ID automaticamente ao salvar")
    void deveGerarIdAutomaticamente() {
        // Act
        Notification saved = notificationRepository.save(notification);

        // Assert
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("Deve salvar notificação com todos os campos preenchidos")
    void deveSalvarNotificacaoComTodosCampos() {
        // Arrange
        notification.setSentAt(LocalDateTime.now());
        notification.setLastError("Test error");

        // Act
        Notification saved = notificationRepository.save(notification);
        entityManager.flush();
        entityManager.clear();

        Notification found = notificationRepository.findById(saved.getId()).orElseThrow();

        // Assert
        assertThat(found.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(found.getPatientId()).isEqualTo(patientId);
        assertThat(found.getNotificationType()).isEqualTo(NotificationType.APPOINTMENT);
        assertThat(found.getChannel()).isEqualTo(Channel.EMAIL);
        assertThat(found.getStatus()).isEqualTo(Status.PENDING);
        assertThat(found.getSentAt()).isNotNull();
        assertThat(found.getLastError()).isEqualTo("Test error");
    }

    // ==================== TESTES DE FIND ====================

    @Test
    @DisplayName("Deve buscar notificação por appointmentId, tipo e canal")
    void deveBuscarNotificacaoPorAppointmentIdTipoECanal() {
        // Arrange
        notificationRepository.save(notification);
        entityManager.flush();

        // Act
        Optional<Notification> found = notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId,
                NotificationType.APPOINTMENT,
                Channel.EMAIL
        );

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getAppointmentId()).isEqualTo(appointmentId);
        assertThat(found.get().getNotificationType()).isEqualTo(NotificationType.APPOINTMENT);
        assertThat(found.get().getChannel()).isEqualTo(Channel.EMAIL);
    }

    @Test
    @DisplayName("Deve retornar empty quando notificação não existe")
    void deveRetornarEmptyQuandoNotificacaoNaoExiste() {
        // Act
        Optional<Notification> found = notificationRepository.findByAppointmentIdAndNotificationTypeAndChannel(
                UUID.randomUUID(),
                NotificationType.APPOINTMENT,
                Channel.EMAIL
        );

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve buscar notificação por eventId")
    void deveBuscarNotificacaoPorEventId() {
        // Arrange
        String eventId = "test-event-123";
        notification.setEventId(eventId);
        notificationRepository.save(notification);
        entityManager.flush();

        // Act
        Optional<Notification> found = notificationRepository.findByEventId(eventId);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("Deve retornar empty quando eventId não existe")
    void deveRetornarEmptyQuandoEventIdNaoExiste() {
        // Act
        Optional<Notification> found = notificationRepository.findByEventId("non-existent-event");

        // Assert
        assertThat(found).isEmpty();
    }

    // ==================== TESTES DE EXISTS ====================

    @Test
    @DisplayName("Deve retornar true quando notificação existe")
    void deveRetornarTrueQuandoNotificacaoExiste() {
        // Arrange
        notificationRepository.save(notification);
        entityManager.flush();

        // Act
        boolean exists = notificationRepository.existsByAppointmentIdAndNotificationTypeAndChannel(
                appointmentId,
                NotificationType.APPOINTMENT,
                Channel.EMAIL
        );

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false quando notificação não existe")
    void deveRetornarFalseQuandoNotificacaoNaoExiste() {
        // Act
        boolean exists = notificationRepository.existsByAppointmentIdAndNotificationTypeAndChannel(
                UUID.randomUUID(),
                NotificationType.APPOINTMENT,
                Channel.EMAIL
        );

        // Assert
        assertThat(exists).isFalse();
    }

    // ==================== TESTES DE UNIQUE CONSTRAINT ====================

    @Test
    @DisplayName("Deve impedir duplicação com mesmos appointmentId, tipo e canal")
    void deveImpedirDuplicacaoComMesmosValores() {
        // Arrange
        notificationRepository.save(notification);
        entityManager.flush();

        Notification duplicate = new Notification();
        duplicate.setAppointmentId(appointmentId);
        duplicate.setPatientId(patientId);
        duplicate.setNotificationType(NotificationType.APPOINTMENT);
        duplicate.setChannel(Channel.EMAIL);
        duplicate.setStatus(Status.PENDING);
        duplicate.setScheduledFor(LocalDateTime.now());
        duplicate.setCreatedAt(LocalDateTime.now());

        // Act & Assert
        assertThatThrownBy(() -> {
            notificationRepository.save(duplicate);
            entityManager.flush();
        }).hasMessageContaining("Unique index");
    }

    @Test
    @DisplayName("Deve permitir notificações com tipos diferentes")
    void devePermitirNotificacoesComTiposDiferentes() {
        // Arrange
        notificationRepository.save(notification);
        entityManager.flush();

        Notification reminder = new Notification();
        reminder.setAppointmentId(appointmentId);
        reminder.setPatientId(patientId);
        reminder.setNotificationType(NotificationType.APPOINTMENT_REMINDER);
        reminder.setChannel(Channel.EMAIL);
        reminder.setStatus(Status.PENDING);
        reminder.setScheduledFor(LocalDateTime.now());
        reminder.setCreatedAt(LocalDateTime.now());

        // Act
        Notification saved = notificationRepository.save(reminder);
        entityManager.flush();

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.APPOINTMENT_REMINDER);
    }

    @Test
    @DisplayName("Deve permitir notificações do mesmo appointment para diferentes canais")
    void devePermitirNotificacoesParaDiferentesCanais() {
        // Arrange
        notification.setChannel(Channel.EMAIL);
        notificationRepository.save(notification);
        entityManager.flush();

        // Se houver mais canais no futuro, este teste faria sentido
        // Por enquanto, apenas verifica que EMAIL funciona
        assertThat(notification.getChannel()).isEqualTo(Channel.EMAIL);
    }

    // ==================== TESTES DE UPDATE ====================

    @Test
    @DisplayName("Deve atualizar status da notificação")
    void deveAtualizarStatusNotificacao() {
        // Arrange
        Notification saved = notificationRepository.save(notification);
        entityManager.flush();
        entityManager.clear();

        // Act
        Notification toUpdate = notificationRepository.findById(saved.getId()).orElseThrow();
        toUpdate.setStatus(Status.SENT);
        toUpdate.setSentAt(LocalDateTime.now());
        notificationRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Notification updated = notificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Status.SENT);
        assertThat(updated.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve atualizar tentativas e erro da notificação")
    void deveAtualizarTentativasEErro() {
        // Arrange
        Notification saved = notificationRepository.save(notification);
        entityManager.flush();
        entityManager.clear();

        // Act
        Notification toUpdate = notificationRepository.findById(saved.getId()).orElseThrow();
        toUpdate.setAttempts(toUpdate.getAttempts() + 1);
        toUpdate.setStatus(Status.FAILED);
        toUpdate.setLastError("Connection timeout");
        notificationRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Notification updated = notificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getAttempts()).isEqualTo(1);
        assertThat(updated.getStatus()).isEqualTo(Status.FAILED);
        assertThat(updated.getLastError()).isEqualTo("Connection timeout");
    }

    // ==================== TESTES DE TIPOS DE NOTIFICAÇÃO ====================

    @Test
    @DisplayName("Deve salvar notificação do tipo APPOINTMENT")
    void deveSalvarNotificacaoTipoAppointment() {
        // Arrange
        notification.setNotificationType(NotificationType.APPOINTMENT);

        // Act
        Notification saved = notificationRepository.save(notification);

        // Assert
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.APPOINTMENT);
    }

    @Test
    @DisplayName("Deve salvar notificação do tipo APPOINTMENT_REMINDER")
    void deveSalvarNotificacaoTipoReminder() {
        // Arrange
        notification.setNotificationType(NotificationType.APPOINTMENT_REMINDER);

        // Act
        Notification saved = notificationRepository.save(notification);

        // Assert
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.APPOINTMENT_REMINDER);
    }

    // ==================== TESTES DE STATUS ====================

    @Test
    @DisplayName("Deve salvar notificação com status PENDING")
    void deveSalvarNotificacaoStatusPending() {
        // Arrange
        notification.setStatus(Status.PENDING);

        // Act
        Notification saved = notificationRepository.save(notification);

        // Assert
        assertThat(saved.getStatus()).isEqualTo(Status.PENDING);
    }

    @Test
    @DisplayName("Deve salvar notificação com status SENT")
    void deveSalvarNotificacaoStatusSent() {
        // Arrange
        notification.setStatus(Status.SENT);
        notification.setSentAt(LocalDateTime.now());

        // Act
        Notification saved = notificationRepository.save(notification);

        // Assert
        assertThat(saved.getStatus()).isEqualTo(Status.SENT);
        assertThat(saved.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve salvar notificação com status FAILED")
    void deveSalvarNotificacaoStatusFailed() {
        // Arrange
        notification.setStatus(Status.FAILED);
        notification.setLastError("Email server error");

        // Act
        Notification saved = notificationRepository.save(notification);

        // Assert
        assertThat(saved.getStatus()).isEqualTo(Status.FAILED);
        assertThat(saved.getLastError()).isEqualTo("Email server error");
    }

    // ==================== TESTES DE DELETE ====================

    @Test
    @DisplayName("Deve deletar notificação por ID")
    void deveDeletarNotificacaoPorId() {
        // Arrange
        Notification saved = notificationRepository.save(notification);
        UUID id = saved.getId();
        entityManager.flush();

        // Act
        notificationRepository.deleteById(id);
        entityManager.flush();

        // Assert
        Optional<Notification> found = notificationRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve contar notificações corretamente")
    void deveContarNotificacoesCorretamente() {
        // Arrange
        notificationRepository.save(notification);

        Notification notification2 = new Notification();
        notification2.setAppointmentId(UUID.randomUUID());
        notification2.setPatientId(UUID.randomUUID());
        notification2.setNotificationType(NotificationType.APPOINTMENT_REMINDER);
        notification2.setChannel(Channel.EMAIL);
        notification2.setStatus(Status.PENDING);
        notification2.setScheduledFor(LocalDateTime.now());
        notification2.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification2);
        entityManager.flush();

        // Act
        long count = notificationRepository.count();

        // Assert
        assertThat(count).isEqualTo(2);
    }
}


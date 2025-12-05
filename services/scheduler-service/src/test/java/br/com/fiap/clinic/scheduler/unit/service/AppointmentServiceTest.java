package br.com.fiap.clinic.scheduler.unit.service;

import br.com.fiap.clinic.scheduler.domain.entity.*;
import br.com.fiap.clinic.scheduler.domain.repository.*;
import br.com.fiap.clinic.scheduler.domain.service.*;
import br.com.fiap.clinic.scheduler.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - AppointmentService")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientService patientService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private UserService userService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AppointmentHistoryRepository appointmentHistoryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AppointmentService appointmentService;

    private UUID patientId;
    private UUID doctorId;
    private UUID userId;
    private Patient patient;
    private Doctor doctor;
    private User nurse;
    private Appointment appointment;
    private OffsetDateTime validStartTime;
    private OffsetDateTime validEndTime;

    /**
     * Calcula o próximo dia útil (segunda a sexta) com pelo menos 2 dias de antecedência.
     */
    private OffsetDateTime getNextBusinessDay() {
        // Começa 2 dias no futuro para garantir antecedência mínima
        OffsetDateTime date = OffsetDateTime.now().plusDays(2);

        // Se cair no fim de semana, avança até chegar em Segunda-feira
        while (date.getDayOfWeek().getValue() >= 6) { // 6=Sábado, 7=Domingo
            date = date.plusDays(1);
        }

        // Força o horário para 10:00 AM para evitar erros de "fora do horário comercial"
        return date.withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        userId = UUID.randomUUID();

        // Setup Patient
        patient = new Patient();
        patient.setId(patientId);
        patient.setName("Paciente Teste");
        patient.setEmail("paciente@test.com");
        patient.setActive(true);
        patient.setRole(Role.patient);
        patient.setBirthDate(LocalDate.of(1990, 1, 1));

        // Setup Doctor
        doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setName("Dr. Teste");
        doctor.setEmail("doctor@test.com");
        doctor.setActive(true);
        doctor.setCrm("CRM123");
        doctor.setSpecialty("Cardiologia");
        doctor.setRole(Role.doctor);

        // Setup Nurse (Creator)
        nurse = new User();
        nurse.setId(userId);
        nurse.setName("Enfermeira Teste");
        nurse.setRole(Role.nurse);

        // Valid times - segunda-feira às 10h, com mais de 1h de antecedência
        validStartTime = getNextBusinessDay();
        validEndTime = validStartTime.plusHours(1);

        // Setup Appointment
        appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setCreatedBy(nurse);
        appointment.setStartAt(validStartTime);
        appointment.setEndAt(validEndTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setActive(true);
    }

    // ==================== TESTES DE BUSCA ====================

    @Test
    @DisplayName("Deve encontrar agendamento por ID com sucesso")
    void deveBuscarAgendamentoPorId() {
        // Arrange
        UUID appointmentId = appointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        // Act
        Appointment found = appointmentService.findById(appointmentId);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(appointmentId);
        verify(appointmentRepository).findById(appointmentId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando agendamento não for encontrado")
    void deveLancarExcecaoQuandoAgendamentoNaoEncontrado() {
        // Arrange
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.findById(appointmentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Consulta não encontrada");

        verify(appointmentRepository).findById(appointmentId);
    }

    @Test
    @DisplayName("Deve listar todos agendamentos para enfermeiro")
    void deveListarTodosAgendamentosParaEnfermeiro() {
        // Arrange
        when(appointmentRepository.findAll()).thenReturn(List.of(appointment));

        // Act
        List<Appointment> appointments = appointmentService.findAll(nurse);

        // Assert
        assertThat(appointments).hasSize(1);
        verify(appointmentRepository).findAll();
    }

    @Test
    @DisplayName("Deve listar apenas agendamentos do paciente quando role é patient")
    void deveListarApenasAgendamentosDoPaciente() {
        // Arrange
        when(appointmentRepository.findByPatient_IdAndIsActiveTrue(patientId))
                .thenReturn(List.of(appointment));

        // Act
        List<Appointment> appointments = appointmentService.findAll(patient);

        // Assert
        assertThat(appointments).hasSize(1);
        verify(appointmentRepository).findByPatient_IdAndIsActiveTrue(patientId);
        verify(appointmentRepository, never()).findAll();
    }

    @Test
    @DisplayName("Deve listar apenas agendamentos do médico quando role é doctor")
    void deveListarApenasAgendamentosDoMedico() {
        // Arrange
        when(appointmentRepository.findByDoctor_IdAndIsActiveTrue(doctorId))
                .thenReturn(List.of(appointment));

        // Act
        List<Appointment> appointments = appointmentService.findAll(doctor);

        // Assert
        assertThat(appointments).hasSize(1);
        verify(appointmentRepository).findByDoctor_IdAndIsActiveTrue(doctorId);
        verify(appointmentRepository, never()).findAll();
    }

    @Test
    @DisplayName("Deve buscar agendamentos por status")
    void deveBuscarAgendamentosPorStatus() {
        // Arrange
        when(appointmentRepository.findByStatusAndIsActiveTrue(AppointmentStatus.SCHEDULED))
                .thenReturn(List.of(appointment));

        // Act
        List<Appointment> appointments = appointmentService.findByStatus(AppointmentStatus.SCHEDULED);

        // Assert
        assertThat(appointments).hasSize(1);
        assertThat(appointments.get(0).getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        verify(appointmentRepository).findByStatusAndIsActiveTrue(AppointmentStatus.SCHEDULED);
    }

    // ==================== TESTES DE CRIAÇÃO ====================

    @Test
    @DisplayName("Deve criar agendamento com sucesso")
    void deveCriarAgendamentoComSucesso() throws Exception {
        // Arrange
        when(patientService.findById(patientId)).thenReturn(patient);
        when(doctorService.findById(doctorId)).thenReturn(doctor);
        when(userService.findById(userId)).thenReturn(nurse);
        when(appointmentRepository.findDoctorConflictingAppointments(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.findPatientConflictingAppointments(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        Appointment created = appointmentService.createAppointment(
                patientId, doctorId, userId, validStartTime, validEndTime);

        // Assert
        assertThat(created).isNotNull();
        verify(patientService).findById(patientId);
        verify(doctorService).findById(doctorId);
        verify(userService).findById(userId);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(appointmentHistoryRepository).save(any(AppointmentHistory.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar agendamento com paciente inativo")
    void deveLancarExcecaoComPacienteInativo() {
        // Arrange
        patient.setActive(false);
        when(patientService.findById(patientId)).thenReturn(patient);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                patientId, doctorId, userId, validStartTime, validEndTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Paciente inativo");

        verify(patientService).findById(patientId);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar agendamento com médico inativo")
    void deveLancarExcecaoComMedicoInativo() {
        // Arrange
        doctor.setActive(false);
        when(patientService.findById(patientId)).thenReturn(patient);
        when(doctorService.findById(doctorId)).thenReturn(doctor);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                patientId, doctorId, userId, validStartTime, validEndTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Médico inativo");

        verify(doctorService).findById(doctorId);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando houver conflito de horário do médico")
    void deveLancarExcecaoComConflitoMedico() {
        // Arrange
        when(patientService.findById(patientId)).thenReturn(patient);
        when(doctorService.findById(doctorId)).thenReturn(doctor);
        when(userService.findById(userId)).thenReturn(nurse);
        when(appointmentRepository.findDoctorConflictingAppointments(any(), any(), any()))
                .thenReturn(List.of(appointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                patientId, doctorId, userId, validStartTime, validEndTime))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Médico já possui consulta agendada");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando houver conflito de horário do paciente")
    void deveLancarExcecaoComConflitoPaciente() {
        // Arrange
        when(patientService.findById(patientId)).thenReturn(patient);
        when(doctorService.findById(doctorId)).thenReturn(doctor);
        when(userService.findById(userId)).thenReturn(nurse);
        when(appointmentRepository.findDoctorConflictingAppointments(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.findPatientConflictingAppointments(any(), any(), any()))
                .thenReturn(List.of(appointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                patientId, doctorId, userId, validStartTime, validEndTime))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Paciente já possui consulta agendada");

        verify(appointmentRepository, never()).save(any());
    }

    // ==================== TESTES DE VALIDAÇÃO DE DATAS ====================

    @Test
    @DisplayName("Deve lançar exceção com horário fora do expediente - antes das 8h")
    void deveLancarExcecaoComHorarioAntesDas8h() {
        // Arrange
        OffsetDateTime startAt = OffsetDateTime.now().plusDays(2).withHour(7).withMinute(30);
        OffsetDateTime endAt = startAt.plusHours(1);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                patientId, doctorId, userId, startAt, endAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Horário de início deve estar entre 8h e 18h");
    }

    @Test
    @DisplayName("Deve lançar exceção com horário fora do expediente - depois das 18h")
    void deveLancarExcecaoComHorarioDepoisDas18h() {
        // Arrange
        OffsetDateTime startAt = OffsetDateTime.now().plusDays(2).withHour(18).withMinute(0);
        OffsetDateTime endAt = startAt.plusHours(1);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                patientId, doctorId, userId, startAt, endAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Horário de início deve estar entre 8h e 18h");
    }

    @Test
    @DisplayName("Deve lançar exceção com data de fim antes do início")
    void deveLancarExcecaoComDataFimAntesInicio() {
        // Arrange
        OffsetDateTime startAt = OffsetDateTime.now().plusDays(2).withHour(11).withMinute(0);
        OffsetDateTime endAt = OffsetDateTime.now().plusDays(2).withHour(10).withMinute(0);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                patientId, doctorId, userId, startAt, endAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data de início deve ser antes da data de fim");
    }

    @Test
    @DisplayName("Deve lançar exceção com antecedência menor que 1 hora")
    void deveLancarExcecaoComAntecedenciaMenorQue1Hora() {
        // Arrange
        OffsetDateTime startAt = OffsetDateTime.now().plusMinutes(30);
        OffsetDateTime endAt = startAt.plusHours(1);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                patientId, doctorId, userId, startAt, endAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agendamento deve ser feito com pelo menos 1 hora de antecedência");
    }

    @Test
    @DisplayName("Deve lançar exceção com duração menor que 15 minutos")
    void deveLancarExcecaoComDuracaoMenorQue15Min() {
        // Arrange
        OffsetDateTime startAt = getNextBusinessDay().plusDays(2).withHour(10).withMinute(0);
        OffsetDateTime endAt = startAt.plusMinutes(10);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                patientId, doctorId, userId, startAt, endAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duração mínima da consulta é 15 minutos");
    }

    @Test
    @DisplayName("Deve lançar exceção com duração maior que 4 horas")
    void deveLancarExcecaoComDuracaoMaiorQue4Horas() {
        // Arrange
        OffsetDateTime startAt = OffsetDateTime.now().plusDays(2).withHour(10).withMinute(0);
        OffsetDateTime endAt = startAt.plusHours(5);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment(
                patientId, doctorId, userId, startAt, endAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duração máxima da consulta é 4 horas");
    }

    // ==================== TESTES DE CONFIRMAÇÃO ====================

    @Test
    @DisplayName("Deve confirmar agendamento com sucesso")
    void deveConfirmarAgendamento() throws Exception {
        // Arrange
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment apt = invocation.getArgument(0);
            apt.setStatus(AppointmentStatus.CONFIRMED);
            return apt;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        Appointment confirmed = appointmentService.confirmAppointment(appointment.getId());

        // Assert
        assertThat(confirmed.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(appointmentHistoryRepository).save(any(AppointmentHistory.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao confirmar agendamento inativo")
    void deveLancarExcecaoAoConfirmarAgendamentoInativo() {
        // Arrange
        appointment.setActive(false);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.confirmAppointment(appointment.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("consulta está inativa");
    }

    // ==================== TESTES DE CANCELAMENTO ====================

    @Test
    @DisplayName("Deve cancelar agendamento com sucesso")
    void deveCancelarAgendamento() throws Exception {
        // Arrange - Agendamento com mais de 24h de antecedência
        appointment.setStartAt(OffsetDateTime.now().plusDays(3).withHour(10).withMinute(0));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment apt = invocation.getArgument(0);
            apt.setStatus(AppointmentStatus.CANCELLED);
            return apt;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        Appointment cancelled = appointmentService.cancelAppointment(appointment.getId());

        // Assert
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar com menos de 24h de antecedência")
    void deveLancarExcecaoAoCancelarComMenosDe24h() {
        // Arrange - Agendamento daqui a 12 horas
        appointment.setStartAt(OffsetDateTime.now().plusHours(12));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancelAppointment(appointment.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cancelamento deve ser feito com pelo menos 24 horas de antecedência");
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar agendamento já completado")
    void deveLancarExcecaoAoCancelarAgendamentoCompletado() {
        // Arrange
        appointment.setStartAt(OffsetDateTime.now().plusDays(3));
        appointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancelAppointment(appointment.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Não é possível cancelar consultas já realizadas ou canceladas");
    }

    // ==================== TESTES DE CONCLUSÃO ====================

    @Test
    @DisplayName("Deve completar agendamento confirmado com sucesso")
    void deveCompletarAgendamento() throws Exception {
        // Arrange
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment apt = invocation.getArgument(0);
            apt.setStatus(AppointmentStatus.COMPLETED);
            return apt;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        Appointment completed = appointmentService.completeAppointment(appointment.getId());

        // Assert
        assertThat(completed.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao completar agendamento não confirmado")
    void deveLancarExcecaoAoCompletarAgendamentoNaoConfirmado() {
        // Arrange
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.completeAppointment(appointment.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Apenas consultas CONFIRMADAS podem ser finalizadas");
    }

    // ==================== TESTES DE REAGENDAMENTO ====================

    @Test
    @DisplayName("Deve reagendar agendamento com sucesso")
    void deveReagendarAgendamento() throws Exception {
        // Arrange
        OffsetDateTime newStart = OffsetDateTime.now().plusDays(4).withHour(14).withMinute(0);
        OffsetDateTime newEnd = newStart.plusHours(1);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.findDoctorConflictingAppointments(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.findPatientConflictingAppointments(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment apt = invocation.getArgument(0);
            apt.setStatus(AppointmentStatus.RESCHEDULED);
            return apt;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        Appointment rescheduled = appointmentService.rescheduleAppointment(
                appointment.getId(), newStart, newEnd);

        // Assert
        assertThat(rescheduled.getStatus()).isEqualTo(AppointmentStatus.RESCHEDULED);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao reagendar agendamento cancelado")
    void deveLancarExcecaoAoReagendarAgendamentoCancelado() {
        // Arrange
        appointment.setStatus(AppointmentStatus.CANCELLED);
        OffsetDateTime newStart = getNextBusinessDay().plusDays(2).withHour(14).withMinute(0);
        OffsetDateTime newEnd = newStart.plusHours(1);

        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(
                appointment.getId(), newStart, newEnd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Não é possível reagendar consultas realizadas ou canceladas");
    }
}


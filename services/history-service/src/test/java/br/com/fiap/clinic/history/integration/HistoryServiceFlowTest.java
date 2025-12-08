package br.com.fiap.clinic.history.integration;

import br.com.fiap.clinic.history.config.security.CustomUserDetails;
import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.repository.ProjectedAppointmentHistoryRepository;
import br.com.fiap.clinic.history.domain.service.HistoryProjectionService;
import br.com.fiap.clinic.history.exception.HistoryAccessDeniedException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("History Service - Integration Flow Test")
class HistoryServiceFlowTest {
    @Autowired
    private HistoryProjectionService historyProjectionService;
    @Autowired
    private ProjectedAppointmentHistoryRepository historyRepository;
    private SecurityContext securityContext;
    private Authentication authentication;
    private UUID patientId1;
    private UUID patientId2;
    private UUID doctorId1;
    private UUID doctorId2;
    private UUID nurseId;

    @BeforeEach
    void setUp() {
        patientId1 = UUID.randomUUID();
        patientId2 = UUID.randomUUID();
        doctorId1 = UUID.randomUUID();
        doctorId2 = UUID.randomUUID();
        nurseId = UUID.randomUUID();

        ProjectedAppointmentHistory history1 = ProjectedAppointmentHistory.builder()
                .patientId(patientId1)
                .patientName("João Silva")
                .doctorId(doctorId1)
                .doctorName("Dr. Maria Santos")
                .startAt(OffsetDateTime.of(2025, 12, 8, 10, 0, 0, 0, ZoneOffset.UTC))
                .status("CONFIRMED")
                .lastAction("CREATED")
                .build();

        ProjectedAppointmentHistory history2 = ProjectedAppointmentHistory.builder()
                .patientId(patientId1)
                .patientName("João Silva")
                .doctorId(doctorId2)
                .doctorName("Dr. Pedro Costa")
                .startAt(OffsetDateTime.of(2025, 12, 9, 14, 0, 0, 0, ZoneOffset.UTC))
                .status("SCHEDULED")
                .lastAction("CREATED")
                .build();

        ProjectedAppointmentHistory history3 = ProjectedAppointmentHistory.builder()
                .patientId(patientId2)
                .patientName("Maria Oliveira")
                .doctorId(doctorId1)
                .doctorName("Dr. Maria Santos")
                .startAt(OffsetDateTime.of(2025, 12, 10, 9, 0, 0, 0, ZoneOffset.UTC))
                .status("CANCELLED")
                .lastAction("CANCELLED")
                .build();

        ProjectedAppointmentHistory history4 = ProjectedAppointmentHistory.builder()
                .patientId(patientId1)
                .patientName("João Silva")
                .doctorId(doctorId1)
                .doctorName("Dr. Maria Santos")
                .startAt(OffsetDateTime.of(2025, 12, 15, 11, 0, 0, 0, ZoneOffset.UTC))
                .status("CONFIRMED")
                .lastAction("CONFIRMED")
                .build();

        historyRepository.save(history1);
        historyRepository.save(history2);
        historyRepository.save(history3);
        historyRepository.save(history4);

        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        historyRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Fluxo: Paciente deve visualizar apenas seu próprio histórico")
    void patientFlowShouldViewOnlyOwnHistory() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_patient"));
        CustomUserDetails userDetails = new CustomUserDetails(patientId1, "patient1@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(null, null, null, null, null);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(h -> h.getPatientId().equals(patientId1));
        assertThat(result).extracting(ProjectedAppointmentHistory::getPatientName).containsOnly("João Silva");
    }

    @Test
    @DisplayName("Fluxo: Paciente não deve ter acesso ao histórico de outro paciente")
    void patientFlowShouldNotAccessOtherPatientHistory() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_patient"));
        CustomUserDetails userDetails = new CustomUserDetails(patientId1, "patient1@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(null, null, null, null, null);

        assertThat(result).noneMatch(h -> h.getPatientId().equals(patientId2));
        assertThat(result).noneMatch(h -> h.getPatientName().equals("Maria Oliveira"));
    }

    @Test
    @DisplayName("Fluxo: Médico deve visualizar histórico de seus pacientes")
    void doctorFlowShouldViewOwnPatientsHistory() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId1, "doctor1@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(null, null, null, null, null);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(h -> h.getDoctorId().equals(doctorId1));
    }

    @Test
    @DisplayName("Fluxo: Médico deve filtrar histórico por paciente específico")
    void doctorFlowShouldFilterBySpecificPatient() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId1, "doctor1@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                patientId2.toString(), null, null, null, null
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPatientId()).isEqualTo(patientId2);
        assertThat(result.getFirst().getDoctorId()).isEqualTo(doctorId1);
    }

    @Test
    @DisplayName("Fluxo: Médico deve filtrar histórico por nome do paciente")
    void doctorFlowShouldFilterByPatientName() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId1, "doctor1@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                null, "João", null, null, null
        );

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(h -> h.getPatientName().contains("João"));
        assertThat(result).allMatch(h -> h.getDoctorId().equals(doctorId1));
    }

    @Test
    @DisplayName("Fluxo: Médico deve filtrar histórico por data específica")
    void doctorFlowShouldFilterBySpecificDate() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId1, "doctor1@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                null, null, null, "2025-12-08", null
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPatientName()).isEqualTo("João Silva");
    }

    @Test
    @DisplayName("Fluxo: Médico deve filtrar histórico por status")
    void doctorFlowShouldFilterByStatus() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId1, "doctor1@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                null, null, null, null, "CONFIRMED"
        );

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(h -> h.getStatus().equals("CONFIRMED"));
    }

    @Test
    @DisplayName("Fluxo: Médico pode buscar consultas de outro médico")
    void doctorFlowCanSearchAnotherDoctorAppointments() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId1, "doctor1@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                null, null, doctorId2.toString(), null, null
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDoctorId()).isEqualTo(doctorId2);
        assertThat(result.getFirst().getDoctorName()).isEqualTo("Dr. Pedro Costa");
    }

    @Test
    @DisplayName("Fluxo: Enfermeiro deve ter acesso amplo ao histórico")
    void nurseFlowShouldHaveBroadAccess() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_nurse"));
        CustomUserDetails userDetails = new CustomUserDetails(nurseId, "nurse@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(null, null, null, null, null);

        assertThat(result).hasSize(4);
    }

    @Test
    @DisplayName("Fluxo: Enfermeiro deve filtrar histórico por paciente")
    void nurseFlowShouldFilterByPatient() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_nurse"));
        CustomUserDetails userDetails = new CustomUserDetails(nurseId, "nurse@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                patientId1.toString(), null, null, null, null
        );

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(h -> h.getPatientId().equals(patientId1));
    }

    @Test
    @DisplayName("Fluxo: Enfermeiro deve filtrar histórico por médico")
    void nurseFlowShouldFilterByDoctor() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_nurse"));
        CustomUserDetails userDetails = new CustomUserDetails(nurseId, "nurse@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                null, null, doctorId1.toString(), null, null
        );

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(h -> h.getDoctorId().equals(doctorId1));
    }

    @Test
    @DisplayName("Fluxo: Enfermeiro deve aplicar múltiplos filtros")
    void nurseFlowShouldApplyMultipleFilters() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_nurse"));
        CustomUserDetails userDetails = new CustomUserDetails(nurseId, "nurse@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                patientId1.toString(), "João", doctorId1.toString(), null, "CONFIRMED"
        );

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(h ->
                h.getPatientId().equals(patientId1) &&
                        h.getDoctorId().equals(doctorId1) &&
                        h.getStatus().equals("CONFIRMED")
        );
    }

    @Test
    @DisplayName("Fluxo: Criar histórico via Kafka deve persistir corretamente")
    void kafkaFlowShouldPersistHistoryCorrectly() {
        ProjectedAppointmentHistory newHistory = ProjectedAppointmentHistory.builder()
                .patientId(UUID.randomUUID())
                .patientName("Carlos Souza")
                .doctorId(UUID.randomUUID())
                .doctorName("Dr. Ana Lima")
                .startAt(OffsetDateTime.now())
                .status("SCHEDULED")
                .lastAction("CREATED")
                .build();

        historyProjectionService.createHistoryFromKafka(newHistory);

        List<ProjectedAppointmentHistory> allHistory = historyRepository.findAll();
        assertThat(allHistory).hasSizeGreaterThan(4);
        assertThat(allHistory).anyMatch(h -> h.getPatientName().equals("Carlos Souza"));
    }

    @Test
    @DisplayName("Fluxo: Validação de entrada nula no Kafka deve lançar exceção")
    void kafkaFlowShouldValidateNullInput() {
        assertThatThrownBy(() -> historyProjectionService.createHistoryFromKafka(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Histórico nulo.");
    }

    @Test
    @DisplayName("Fluxo: Usuário não autenticado não deve ter acesso")
    void securityFlowShouldDenyUnauthenticatedUser() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThatThrownBy(() -> historyProjectionService.getHistory(null, null, null, null, null))
                .isInstanceOf(HistoryAccessDeniedException.class)
                .hasMessage("Usuário não autenticado.");
    }

    @Test
    @DisplayName("Fluxo: Usuário com role inválida não deve ter acesso")
    void securityFlowShouldDenyInvalidRole() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_admin"));
        CustomUserDetails userDetails = new CustomUserDetails(UUID.randomUUID(), "admin@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        assertThatThrownBy(() -> historyProjectionService.getHistory(null, null, null, null, null))
                .isInstanceOf(HistoryAccessDeniedException.class)
                .hasMessage("Acesso negado ao histórico de consultas.");
    }

    @Test
    @DisplayName("Fluxo: Validação de UUID inválido deve lançar exceção")
    void validationFlowShouldRejectInvalidUuid() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId1, "doctor1@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        assertThatThrownBy(() -> historyProjectionService.getHistory("invalid-uuid", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID inválido");
    }

    @Test
    @DisplayName("Fluxo: Validação de data inválida deve lançar exceção")
    void validationFlowShouldRejectInvalidDate() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId1, "doctor1@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        assertThatThrownBy(() -> historyProjectionService.getHistory(null, null, null, "invalid-date", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato de data inválido. Use YYYY-MM-DD");
    }
}


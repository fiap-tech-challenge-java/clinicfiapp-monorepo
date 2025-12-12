package br.com.fiap.clinic.history.unit.service;

import br.com.fiap.clinic.history.config.security.CustomUserDetails;
import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.repository.ProjectedAppointmentHistoryRepository;
import br.com.fiap.clinic.history.domain.service.HistoryProjectionService;
import br.com.fiap.clinic.history.exception.HistoryAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoryProjectionService - Testes Unitários")
class HistoryProjectionServiceTest {

    @Mock
    private ProjectedAppointmentHistoryRepository historyRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private HistoryProjectionService historyProjectionService;

    private UUID patientId;
    private UUID doctorId;
    private UUID nurseId;
    private ProjectedAppointmentHistory history1;
    private ProjectedAppointmentHistory history2;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        nurseId = UUID.randomUUID();

        history1 = ProjectedAppointmentHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .patientName("João Silva")
                .doctorId(doctorId)
                .doctorName("Dr. Maria")
                .startAt(OffsetDateTime.now())
                .status("CONFIRMED")
                .lastAction("CREATED")
                .build();

        history2 = ProjectedAppointmentHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .patientName("João Silva")
                .doctorId(doctorId)
                .doctorName("Dr. Maria")
                .startAt(OffsetDateTime.now().plusDays(1))
                .status("SCHEDULED")
                .lastAction("CREATED")
                .build();

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Deve retornar histórico quando usuário é PACIENTE e busca seus próprios dados")
    void shouldReturnHistoryWhenUserIsPatient() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_patient"));
        CustomUserDetails userDetails = new CustomUserDetails(patientId, "patient@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findByPatientId(patientId)).thenReturn(List.of(history1, history2));

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(null, null, null, null, null);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(history1, history2);
        verify(historyRepository).findByPatientId(patientId);
        verify(historyRepository, never()).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando PACIENTE não tem histórico")
    void shouldReturnEmptyListWhenPatientHasNoHistory() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_patient"));
        CustomUserDetails userDetails = new CustomUserDetails(patientId, "patient@test.com", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findByPatientId(patientId)).thenReturn(new ArrayList<>());

        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(null, null, null, null, null);

        assertThat(result).isEmpty();
        verify(historyRepository).findByPatientId(patientId);
    }

    @Test
    @DisplayName("Deve retornar histórico quando MÉDICO busca sem especificar doctorId (retorna consultas dele)")
    void shouldReturnDoctorOwnHistoryWhenDoctorIdNotSpecified() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId, "doctor@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findAll(any(Specification.class))).thenReturn(List.of(history1, history2));
        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(null, null, null, null, null);
        assertThat(result).hasSize(2);
        verify(historyRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve retornar histórico quando MÉDICO busca por patientId específico")
    void shouldReturnHistoryWhenDoctorSearchesByPatientId() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId, "doctor@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findAll(any(Specification.class))).thenReturn(List.of(history1));
        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                patientId.toString(), null, null, null, null
        );
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPatientId()).isEqualTo(patientId);
        verify(historyRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve retornar histórico quando MÉDICO busca por nome de paciente")
    void shouldReturnHistoryWhenDoctorSearchesByPatientName() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId, "doctor@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findAll(any(Specification.class))).thenReturn(List.of(history1, history2));
        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                null, "João", null, null, null
        );
        assertThat(result).hasSize(2);
        verify(historyRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve retornar histórico quando MÉDICO busca por data específica")
    void shouldReturnHistoryWhenDoctorSearchesByDate() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId, "doctor@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findAll(any(Specification.class))).thenReturn(List.of(history1));
        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                null, null, null, "2025-12-08", null
        );
        assertThat(result).hasSize(1);
        verify(historyRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve retornar histórico quando MÉDICO busca por status")
    void shouldReturnHistoryWhenDoctorSearchesByStatus() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId, "doctor@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findAll(any(Specification.class))).thenReturn(List.of(history1));
        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                null, null, null, null, "CONFIRMED"
        );
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo("CONFIRMED");
        verify(historyRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve retornar histórico quando MÉDICO busca outro médico específico")
    void shouldReturnHistoryWhenDoctorSearchesAnotherDoctor() {
        UUID anotherDoctorId = UUID.randomUUID();
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId, "doctor@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findAll(any(Specification.class))).thenReturn(new ArrayList<>());
        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                null, null, anotherDoctorId.toString(), null, null
        );
        assertThat(result).isEmpty();
        verify(historyRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve retornar histórico quando ENFERMEIRO busca por patientId")
    void shouldReturnHistoryWhenNurseSearchesByPatientId() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_nurse"));
        CustomUserDetails userDetails = new CustomUserDetails(nurseId, "nurse@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findAll(any(Specification.class))).thenReturn(List.of(history1, history2));
        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                patientId.toString(), null, null, null, null
        );
        assertThat(result).hasSize(2);
        verify(historyRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve retornar histórico quando ENFERMEIRO busca sem filtros")
    void shouldReturnHistoryWhenNurseSearchesWithoutFilters() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_nurse"));
        CustomUserDetails userDetails = new CustomUserDetails(nurseId, "nurse@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findAll(any(Specification.class))).thenReturn(List.of(history1, history2));
        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                null, null, null, null, null
        );
        assertThat(result).hasSize(2);
        verify(historyRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve lançar HistoryAccessDeniedException quando usuário não autenticado")
    void shouldThrowExceptionWhenUserNotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
        assertThatThrownBy(() -> historyProjectionService.getHistory(null, null, null, null, null))
                .isInstanceOf(HistoryAccessDeniedException.class)
                .hasMessage("Usuário não autenticado.");
        verify(historyRepository, never()).findByPatientId(any());
        verify(historyRepository, never()).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve lançar HistoryAccessDeniedException quando autenticação é anônima")
    void shouldThrowExceptionWhenAuthenticationIsAnonymous() {
        AnonymousAuthenticationToken anonymousAuth = mock(AnonymousAuthenticationToken.class);
        when(securityContext.getAuthentication()).thenReturn(anonymousAuth);
        when(anonymousAuth.isAuthenticated()).thenReturn(true);
        assertThatThrownBy(() -> historyProjectionService.getHistory(null, null, null, null, null))
                .isInstanceOf(HistoryAccessDeniedException.class)
                .hasMessage("Usuário não autenticado.");
        verify(historyRepository, never()).findByPatientId(any());
        verify(historyRepository, never()).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve lançar HistoryAccessDeniedException quando usuário não tem role válida")
    void shouldThrowExceptionWhenUserHasNoValidRole() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_admin"));
        CustomUserDetails userDetails = new CustomUserDetails(UUID.randomUUID(), "admin@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        assertThatThrownBy(() -> historyProjectionService.getHistory(null, null, null, null, null))
                .isInstanceOf(HistoryAccessDeniedException.class)
                .hasMessage("Acesso negado ao histórico de consultas.");
        verify(historyRepository, never()).findByPatientId(any());
        verify(historyRepository, never()).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando patientId é inválido")
    void shouldThrowExceptionWhenPatientIdIsInvalid() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId, "doctor@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        assertThatThrownBy(() -> historyProjectionService.getHistory(
                "invalid-uuid", null, null, null, null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID inválido");
        verify(historyRepository, never()).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando doctorId é inválido")
    void shouldThrowExceptionWhenDoctorIdIsInvalid() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_nurse"));
        CustomUserDetails userDetails = new CustomUserDetails(nurseId, "nurse@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        assertThatThrownBy(() -> historyProjectionService.getHistory(
                null, null, "invalid-uuid", null, null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID inválido");
        verify(historyRepository, never()).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando formato de data é inválido")
    void shouldThrowExceptionWhenDateFormatIsInvalid() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId, "doctor@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        assertThatThrownBy(() -> historyProjectionService.getHistory(
                null, null, null, "invalid-date", null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Formato de data inválido. Use YYYY-MM-DD");
        verify(historyRepository, never()).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve salvar histórico quando createHistoryFromKafka é chamado com dados válidos")
    void shouldSaveHistoryWhenCreateHistoryFromKafkaIsCalledWithValidData() {
        ProjectedAppointmentHistory history = ProjectedAppointmentHistory.builder()
                .patientId(patientId)
                .patientName("João Silva")
                .doctorId(doctorId)
                .doctorName("Dr. Maria")
                .startAt(OffsetDateTime.now())
                .status("CONFIRMED")
                .lastAction("CREATED")
                .build();
        when(historyRepository.save(any(ProjectedAppointmentHistory.class))).thenReturn(history);
        historyProjectionService.createHistoryFromKafka(history);
        ArgumentCaptor<ProjectedAppointmentHistory> captor = ArgumentCaptor.forClass(ProjectedAppointmentHistory.class);
        verify(historyRepository).save(captor.capture());
        ProjectedAppointmentHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getPatientId()).isEqualTo(patientId);
        assertThat(savedHistory.getDoctorId()).isEqualTo(doctorId);
        assertThat(savedHistory.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando createHistoryFromKafka recebe null")
    void shouldThrowExceptionWhenCreateHistoryFromKafkaReceivesNull() {
        assertThatThrownBy(() -> historyProjectionService.createHistoryFromKafka(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Histórico nulo.");
        verify(historyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar histórico quando MÉDICO busca com múltiplos filtros")
    void shouldReturnHistoryWhenDoctorSearchesWithMultipleFilters() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_doctor"));
        CustomUserDetails userDetails = new CustomUserDetails(doctorId, "doctor@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findAll(any(Specification.class))).thenReturn(List.of(history1));
        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                patientId.toString(), "João", null, "2025-12-08", "CONFIRMED"
        );
        assertThat(result).hasSize(1);
        verify(historyRepository).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há resultados com filtros aplicados")
    void shouldReturnEmptyListWhenNoResultsWithFilters() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_nurse"));
        CustomUserDetails userDetails = new CustomUserDetails(nurseId, "nurse@test.com", authorities);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(historyRepository.findAll(any(Specification.class))).thenReturn(new ArrayList<>());
        List<ProjectedAppointmentHistory> result = historyProjectionService.getHistory(
                UUID.randomUUID().toString(), null, null, null, "CANCELLED"
        );
        assertThat(result).isEmpty();
        verify(historyRepository).findAll(any(Specification.class));
    }
}



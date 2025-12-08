package br.com.fiap.clinic.history.unit.controller;

import br.com.fiap.clinic.history.controller.graphql.HistoryQueryController;
import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.service.HistoryProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoryQueryController - Testes Unitários")
class HistoryQueryControllerTest {

    @Mock
    private HistoryProjectionService historyService;

    @InjectMocks
    private HistoryQueryController historyQueryController;

    private UUID patientId;
    private UUID doctorId;
    private ProjectedAppointmentHistory history1;
    private ProjectedAppointmentHistory history2;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();

        history1 = ProjectedAppointmentHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .patientName("João Silva")
                .doctorId(doctorId)
                .doctorName("Dr. Maria Santos")
                .startAt(OffsetDateTime.now())
                .status("CONFIRMED")
                .lastAction("CREATED")
                .build();

        history2 = ProjectedAppointmentHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .patientName("João Silva")
                .doctorId(doctorId)
                .doctorName("Dr. Maria Santos")
                .startAt(OffsetDateTime.now().plusDays(1))
                .status("SCHEDULED")
                .lastAction("CREATED")
                .build();
    }

    @Test
    @DisplayName("Deve retornar histórico quando todos os parâmetros são fornecidos")
    void shouldReturnHistoryWhenAllParametersProvided() {
        String patientIdStr = patientId.toString();
        String doctorIdStr = doctorId.toString();
        String patientName = "João Silva";
        String date = "2025-12-08";
        String status = "CONFIRMED";

        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1, history2);
        when(historyService.getHistory(patientIdStr, patientName, doctorIdStr, date, status))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                patientIdStr, patientName, doctorIdStr, date, status
        );

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(history1, history2);
        verify(historyService).getHistory(patientIdStr, patientName, doctorIdStr, date, status);
    }

    @Test
    @DisplayName("Deve retornar histórico quando apenas patientId é fornecido")
    void shouldReturnHistoryWhenOnlyPatientIdProvided() {
        String patientIdStr = patientId.toString();
        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1, history2);

        when(historyService.getHistory(patientIdStr, null, null, null, null))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                patientIdStr, null, null, null, null
        );

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(h -> h.getPatientId().equals(patientId));
        verify(historyService).getHistory(patientIdStr, null, null, null, null);
    }

    @Test
    @DisplayName("Deve retornar histórico quando apenas doctorId é fornecido")
    void shouldReturnHistoryWhenOnlyDoctorIdProvided() {
        String doctorIdStr = doctorId.toString();
        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1);

        when(historyService.getHistory(null, null, doctorIdStr, null, null))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                null, null, doctorIdStr, null, null
        );

        assertThat(result).hasSize(1);
        assertThat(result).allMatch(h -> h.getDoctorId().equals(doctorId));
        verify(historyService).getHistory(null, null, doctorIdStr, null, null);
    }

    @Test
    @DisplayName("Deve retornar histórico quando apenas patientName é fornecido")
    void shouldReturnHistoryWhenOnlyPatientNameProvided() {
        String patientName = "João";
        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1, history2);

        when(historyService.getHistory(null, patientName, null, null, null))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                null, patientName, null, null, null
        );

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(h -> h.getPatientName().contains("João"));
        verify(historyService).getHistory(null, patientName, null, null, null);
    }

    @Test
    @DisplayName("Deve retornar histórico quando apenas date é fornecido")
    void shouldReturnHistoryWhenOnlyDateProvided() {
        String date = "2025-12-08";
        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1);

        when(historyService.getHistory(null, null, null, date, null))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                null, null, null, date, null
        );

        assertThat(result).hasSize(1);
        verify(historyService).getHistory(null, null, null, date, null);
    }

    @Test
    @DisplayName("Deve retornar histórico quando apenas status é fornecido")
    void shouldReturnHistoryWhenOnlyStatusProvided() {
        String status = "CONFIRMED";
        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1);

        when(historyService.getHistory(null, null, null, null, status))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                null, null, null, null, status
        );

        assertThat(result).hasSize(1);
        assertThat(result).allMatch(h -> h.getStatus().equals("CONFIRMED"));
        verify(historyService).getHistory(null, null, null, null, status);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando nenhum histórico é encontrado")
    void shouldReturnEmptyListWhenNoHistoryFound() {
        when(historyService.getHistory(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ArrayList<>());

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                patientId.toString(), "Test", doctorId.toString(), "2025-12-08", "CONFIRMED"
        );

        assertThat(result).isEmpty();
        verify(historyService).getHistory(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve retornar histórico quando nenhum parâmetro é fornecido")
    void shouldReturnHistoryWhenNoParametersProvided() {
        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1, history2);

        when(historyService.getHistory(null, null, null, null, null))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                null, null, null, null, null
        );

        assertThat(result).hasSize(2);
        verify(historyService).getHistory(null, null, null, null, null);
    }

    @Test
    @DisplayName("Deve retornar histórico com diferentes status")
    void shouldReturnHistoryWithDifferentStatuses() {
        ProjectedAppointmentHistory cancelledHistory = ProjectedAppointmentHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .patientName("João Silva")
                .doctorId(doctorId)
                .doctorName("Dr. Maria Santos")
                .startAt(OffsetDateTime.now())
                .status("CANCELLED")
                .lastAction("CANCELLED")
                .build();

        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1, cancelledHistory);
        when(historyService.getHistory(null, null, null, null, null))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                null, null, null, null, null
        );

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProjectedAppointmentHistory::getStatus)
                .containsExactly("CONFIRMED", "CANCELLED");
        verify(historyService).getHistory(null, null, null, null, null);
    }

    @Test
    @DisplayName("Deve retornar histórico filtrado por patientId e date")
    void shouldReturnHistoryFilteredByPatientIdAndDate() {
        String patientIdStr = patientId.toString();
        String date = "2025-12-08";
        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1);

        when(historyService.getHistory(patientIdStr, null, null, date, null))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                patientIdStr, null, null, date, null
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPatientId()).isEqualTo(patientId);
        verify(historyService).getHistory(patientIdStr, null, null, date, null);
    }

    @Test
    @DisplayName("Deve retornar histórico filtrado por doctorId e status")
    void shouldReturnHistoryFilteredByDoctorIdAndStatus() {
        String doctorIdStr = doctorId.toString();
        String status = "CONFIRMED";
        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1);

        when(historyService.getHistory(null, null, doctorIdStr, null, status))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                null, null, doctorIdStr, null, status
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDoctorId()).isEqualTo(doctorId);
        assertThat(result.getFirst().getStatus()).isEqualTo("CONFIRMED");
        verify(historyService).getHistory(null, null, doctorIdStr, null, status);
    }

    @Test
    @DisplayName("Deve retornar histórico filtrado por patientName e status")
    void shouldReturnHistoryFilteredByPatientNameAndStatus() {
        String patientName = "João";
        String status = "SCHEDULED";
        List<ProjectedAppointmentHistory> expectedHistory = List.of(history2);

        when(historyService.getHistory(null, patientName, null, null, status))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                null, patientName, null, null, status
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPatientName()).contains("João");
        assertThat(result.getFirst().getStatus()).isEqualTo("SCHEDULED");
        verify(historyService).getHistory(null, patientName, null, null, status);
    }

    @Test
    @DisplayName("Deve retornar múltiplos históricos para o mesmo paciente")
    void shouldReturnMultipleHistoriesForSamePatient() {
        String patientIdStr = patientId.toString();

        ProjectedAppointmentHistory history3 = ProjectedAppointmentHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .patientName("João Silva")
                .doctorId(UUID.randomUUID())
                .doctorName("Dr. Pedro Costa")
                .startAt(OffsetDateTime.now().plusDays(2))
                .status("SCHEDULED")
                .lastAction("CREATED")
                .build();

        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1, history2, history3);
        when(historyService.getHistory(patientIdStr, null, null, null, null))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                patientIdStr, null, null, null, null
        );

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(h -> h.getPatientId().equals(patientId));
        assertThat(result).extracting(ProjectedAppointmentHistory::getPatientName)
                .containsOnly("João Silva");
        verify(historyService).getHistory(patientIdStr, null, null, null, null);
    }

    @Test
    @DisplayName("Deve retornar histórico com caracteres especiais no nome")
    void shouldReturnHistoryWithSpecialCharactersInName() {
        ProjectedAppointmentHistory historyWithSpecialChars = ProjectedAppointmentHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .patientName("José María González")
                .doctorId(doctorId)
                .doctorName("Dra. Françoise D'Ávila")
                .startAt(OffsetDateTime.now())
                .status("CONFIRMED")
                .lastAction("CREATED")
                .build();

        List<ProjectedAppointmentHistory> expectedHistory = List.of(historyWithSpecialChars);
        when(historyService.getHistory(null, "José", null, null, null))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                null, "José", null, null, null
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPatientName()).isEqualTo("José María González");
        assertThat(result.getFirst().getDoctorName()).isEqualTo("Dra. Françoise D'Ávila");
        verify(historyService).getHistory(null, "José", null, null, null);
    }

    @Test
    @DisplayName("Deve delegar corretamente todos os parâmetros para o service")
    void shouldDelegateAllParametersToService() {
        String patientIdStr = "patient-123";
        String patientName = "Test Patient";
        String doctorIdStr = "doctor-456";
        String date = "2025-12-10";
        String status = "COMPLETED";

        when(historyService.getHistory(patientIdStr, patientName, doctorIdStr, date, status))
                .thenReturn(new ArrayList<>());

        historyQueryController.history(patientIdStr, patientName, doctorIdStr, date, status);

        verify(historyService).getHistory(
                eq(patientIdStr),
                eq(patientName),
                eq(doctorIdStr),
                eq(date),
                eq(status)
        );
    }

    @Test
    @DisplayName("Deve retornar histórico com diferentes lastActions")
    void shouldReturnHistoryWithDifferentLastActions() {
        ProjectedAppointmentHistory updatedHistory = ProjectedAppointmentHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .patientName("João Silva")
                .doctorId(doctorId)
                .doctorName("Dr. Maria Santos")
                .startAt(OffsetDateTime.now())
                .status("CONFIRMED")
                .lastAction("UPDATED")
                .build();

        List<ProjectedAppointmentHistory> expectedHistory = List.of(history1, updatedHistory);
        when(historyService.getHistory(null, null, null, null, null))
                .thenReturn(expectedHistory);

        List<ProjectedAppointmentHistory> result = historyQueryController.history(
                null, null, null, null, null
        );

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProjectedAppointmentHistory::getLastAction)
                .containsExactly("CREATED", "UPDATED");
        verify(historyService).getHistory(null, null, null, null, null);
    }
}


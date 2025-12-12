package br.com.fiap.clinic.history.unit.listener;

import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.repository.ProcessedEventRepository;
import br.com.fiap.clinic.history.domain.service.HistoryProjectionService;
import br.com.fiap.clinic.history.listener.AppointmentEventConsumer;
import br.com.fiap.clinic.history.listener.KafkaEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventConsumer - Testes Unitários")
class KafkaEventConsumerTest {

    @Mock
    private HistoryProjectionService historyService;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private KafkaEventConsumer kafkaEventConsumer;

    private AppointmentEventConsumer validEvent;
    private UUID eventId;
    private UUID patientId;
    private UUID doctorId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();

        validEvent = new AppointmentEventConsumer();
        validEvent.setEventId(eventId.toString());
        validEvent.setAppointmentId(UUID.randomUUID().toString());
        validEvent.setPatientId(patientId.toString());
        validEvent.setDoctorId(doctorId.toString());
        validEvent.setDoctorName("Dr. Maria Santos");
        validEvent.setPatientName("João Silva");
        validEvent.setPatientEmail("joao@test.com");
        validEvent.setStatus("CONFIRMED");
        validEvent.setEventType("CREATED");
        validEvent.setTimestamp("2025-12-08T10:00:00Z");
        validEvent.setAppointmentDate("2025-12-08T10:00:00-03:00");
    }

    @Test
    @DisplayName("Deve processar evento válido com sucesso")
    void shouldProcessValidEventSuccessfully() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        doNothing().when(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));

        kafkaEventConsumer.listen(validEvent);

        ArgumentCaptor<ProjectedAppointmentHistory> captor = ArgumentCaptor.forClass(ProjectedAppointmentHistory.class);
        verify(historyService).createHistoryFromKafka(captor.capture());

        ProjectedAppointmentHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getPatientId()).isEqualTo(patientId);
        assertThat(savedHistory.getDoctorId()).isEqualTo(doctorId);
        assertThat(savedHistory.getPatientName()).isEqualTo("João Silva");
        assertThat(savedHistory.getDoctorName()).isEqualTo("Dr. Maria Santos");
        assertThat(savedHistory.getStatus()).isEqualTo("CONFIRMED");
        assertThat(savedHistory.getLastAction()).isEqualTo("CREATED");
        assertThat(savedHistory.getStartAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve ignorar evento duplicado quando eventId já foi processado")
    void shouldIgnoreDuplicateEvent() {
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        kafkaEventConsumer.listen(validEvent);

        verify(processedEventRepository).existsById(eventId);
        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve processar evento sem eventId com aviso de idempotência")
    void shouldProcessEventWithoutEventIdWithWarning() {
        validEvent.setEventId(null);
        doNothing().when(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));

        kafkaEventConsumer.listen(validEvent);

        verify(processedEventRepository, never()).existsById(any());
        verify(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));
    }

    @Test
    @DisplayName("Deve ignorar evento quando patientId é null")
    void shouldIgnoreEventWhenPatientIdIsNull() {
        validEvent.setPatientId(null);

        kafkaEventConsumer.listen(validEvent);

        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve ignorar evento quando patientId é vazio")
    void shouldIgnoreEventWhenPatientIdIsEmpty() {
        validEvent.setPatientId("");

        kafkaEventConsumer.listen(validEvent);

        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve ignorar evento quando doctorId é null")
    void shouldIgnoreEventWhenDoctorIdIsNull() {
        validEvent.setDoctorId(null);

        kafkaEventConsumer.listen(validEvent);

        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve ignorar evento quando doctorId é vazio")
    void shouldIgnoreEventWhenDoctorIdIsEmpty() {
        validEvent.setDoctorId("");

        kafkaEventConsumer.listen(validEvent);

        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve ignorar evento quando patientId não é UUID válido")
    void shouldIgnoreEventWhenPatientIdIsInvalidUUID() {
        validEvent.setPatientId("invalid-uuid");

        kafkaEventConsumer.listen(validEvent);

        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve ignorar evento quando doctorId não é UUID válido")
    void shouldIgnoreEventWhenDoctorIdIsInvalidUUID() {
        validEvent.setDoctorId("invalid-uuid");

        kafkaEventConsumer.listen(validEvent);

        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve ignorar evento quando appointmentDate é null")
    void shouldIgnoreEventWhenAppointmentDateIsNull() {
        validEvent.setAppointmentDate(null);

        kafkaEventConsumer.listen(validEvent);

        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve ignorar evento quando appointmentDate é vazio")
    void shouldIgnoreEventWhenAppointmentDateIsEmpty() {
        validEvent.setAppointmentDate("");

        kafkaEventConsumer.listen(validEvent);

        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve ignorar evento quando appointmentDate tem formato inválido")
    void shouldIgnoreEventWhenAppointmentDateHasInvalidFormat() {
        validEvent.setAppointmentDate("invalid-date-format");

        kafkaEventConsumer.listen(validEvent);

        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve converter data para timezone de Brasília corretamente")
    void shouldConvertDateToBrasiliaTimezoneCorrectly() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        validEvent.setAppointmentDate("2025-12-08T10:00:00Z");
        doNothing().when(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));

        kafkaEventConsumer.listen(validEvent);

        ArgumentCaptor<ProjectedAppointmentHistory> captor = ArgumentCaptor.forClass(ProjectedAppointmentHistory.class);
        verify(historyService).createHistoryFromKafka(captor.capture());

        ProjectedAppointmentHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getStartAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve processar evento com data no formato ISO_DATE_TIME")
    void shouldProcessEventWithIsoDateTimeFormat() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        validEvent.setAppointmentDate("2025-12-08T14:30:00-03:00");
        doNothing().when(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));

        kafkaEventConsumer.listen(validEvent);

        ArgumentCaptor<ProjectedAppointmentHistory> captor = ArgumentCaptor.forClass(ProjectedAppointmentHistory.class);
        verify(historyService).createHistoryFromKafka(captor.capture());

        ProjectedAppointmentHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getStartAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve mapear todos os campos do evento para ProjectedAppointmentHistory")
    void shouldMapAllEventFieldsToProjectedHistory() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        doNothing().when(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));

        kafkaEventConsumer.listen(validEvent);

        ArgumentCaptor<ProjectedAppointmentHistory> captor = ArgumentCaptor.forClass(ProjectedAppointmentHistory.class);
        verify(historyService).createHistoryFromKafka(captor.capture());

        ProjectedAppointmentHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getPatientId()).isEqualTo(patientId);
        assertThat(savedHistory.getDoctorId()).isEqualTo(doctorId);
        assertThat(savedHistory.getPatientName()).isEqualTo(validEvent.getPatientName());
        assertThat(savedHistory.getDoctorName()).isEqualTo(validEvent.getDoctorName());
        assertThat(savedHistory.getStatus()).isEqualTo(validEvent.getStatus());
        assertThat(savedHistory.getLastAction()).isEqualTo(validEvent.getEventType());
    }

    @Test
    @DisplayName("Deve processar evento com status SCHEDULED")
    void shouldProcessEventWithScheduledStatus() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        validEvent.setStatus("SCHEDULED");
        validEvent.setEventType("SCHEDULED");
        doNothing().when(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));

        kafkaEventConsumer.listen(validEvent);

        ArgumentCaptor<ProjectedAppointmentHistory> captor = ArgumentCaptor.forClass(ProjectedAppointmentHistory.class);
        verify(historyService).createHistoryFromKafka(captor.capture());

        ProjectedAppointmentHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getStatus()).isEqualTo("SCHEDULED");
        assertThat(savedHistory.getLastAction()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("Deve processar evento com status CANCELLED")
    void shouldProcessEventWithCancelledStatus() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        validEvent.setStatus("CANCELLED");
        validEvent.setEventType("CANCELLED");
        doNothing().when(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));

        kafkaEventConsumer.listen(validEvent);

        ArgumentCaptor<ProjectedAppointmentHistory> captor = ArgumentCaptor.forClass(ProjectedAppointmentHistory.class);
        verify(historyService).createHistoryFromKafka(captor.capture());

        ProjectedAppointmentHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getStatus()).isEqualTo("CANCELLED");
        assertThat(savedHistory.getLastAction()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Deve processar evento com status COMPLETED")
    void shouldProcessEventWithCompletedStatus() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        validEvent.setStatus("COMPLETED");
        validEvent.setEventType("COMPLETED");
        doNothing().when(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));

        kafkaEventConsumer.listen(validEvent);

        ArgumentCaptor<ProjectedAppointmentHistory> captor = ArgumentCaptor.forClass(ProjectedAppointmentHistory.class);
        verify(historyService).createHistoryFromKafka(captor.capture());

        ProjectedAppointmentHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getStatus()).isEqualTo("COMPLETED");
        assertThat(savedHistory.getLastAction()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Deve continuar processamento mesmo com exceção no historyService")
    void shouldContinueProcessingEvenWithExceptionInHistoryService() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        doThrow(new RuntimeException("Database error")).when(historyService).createHistoryFromKafka(any());

        assertThatCode(() -> kafkaEventConsumer.listen(validEvent))
                .doesNotThrowAnyException();

        verify(historyService).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve lidar com exceção ao verificar evento duplicado")
    void shouldHandleExceptionWhenCheckingDuplicateEvent() {
        when(processedEventRepository.existsById(eventId)).thenThrow(new RuntimeException("Repository error"));

        assertThatCode(() -> kafkaEventConsumer.listen(validEvent))
                .doesNotThrowAnyException();

        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve processar evento com nomes contendo caracteres especiais")
    void shouldProcessEventWithSpecialCharactersInNames() {
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        validEvent.setPatientName("José da Silva Júnior");
        validEvent.setDoctorName("Dra. María Rodríguez");
        doNothing().when(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));

        kafkaEventConsumer.listen(validEvent);

        ArgumentCaptor<ProjectedAppointmentHistory> captor = ArgumentCaptor.forClass(ProjectedAppointmentHistory.class);
        verify(historyService).createHistoryFromKafka(captor.capture());

        ProjectedAppointmentHistory savedHistory = captor.getValue();
        assertThat(savedHistory.getPatientName()).isEqualTo("José da Silva Júnior");
        assertThat(savedHistory.getDoctorName()).isEqualTo("Dra. María Rodríguez");
    }

    @Test
    @DisplayName("Deve processar múltiplos eventos sequencialmente")
    void shouldProcessMultipleEventsSequentially() {
        when(processedEventRepository.existsById(any())).thenReturn(false);
        doNothing().when(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));

        AppointmentEventConsumer event1 = createEventWithId(UUID.randomUUID());
        AppointmentEventConsumer event2 = createEventWithId(UUID.randomUUID());
        AppointmentEventConsumer event3 = createEventWithId(UUID.randomUUID());

        kafkaEventConsumer.listen(event1);
        kafkaEventConsumer.listen(event2);
        kafkaEventConsumer.listen(event3);

        verify(historyService, times(3)).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));
    }

    @Test
    @DisplayName("Deve ignorar evento com eventId vazio pois gera exceção ao converter UUID")
    void shouldIgnoreEventWithEmptyEventId() {
        validEvent.setEventId("");

        kafkaEventConsumer.listen(validEvent);

        verify(processedEventRepository, never()).existsById(any());
        verify(historyService, never()).createHistoryFromKafka(any());
    }

    @Test
    @DisplayName("Deve processar evento com diferentes tipos de eventType")
    void shouldProcessEventWithDifferentEventTypes() {
        doNothing().when(historyService).createHistoryFromKafka(any(ProjectedAppointmentHistory.class));

        String[] eventTypes = {"CREATED", "UPDATED", "CANCELLED", "COMPLETED", "RESCHEDULED"};

        for (String eventType : eventTypes) {
            UUID newEventId = UUID.randomUUID();
            when(processedEventRepository.existsById(newEventId)).thenReturn(false);

            validEvent.setEventType(eventType);
            validEvent.setEventId(newEventId.toString());

            kafkaEventConsumer.listen(validEvent);
        }

        ArgumentCaptor<ProjectedAppointmentHistory> captor = ArgumentCaptor.forClass(ProjectedAppointmentHistory.class);
        verify(historyService, times(5)).createHistoryFromKafka(captor.capture());

        List<ProjectedAppointmentHistory> capturedHistories = captor.getAllValues();
        assertThat(capturedHistories).hasSize(5);

        for (int i = 0; i < eventTypes.length; i++) {
            assertThat(capturedHistories.get(i).getLastAction()).isEqualTo(eventTypes[i]);
        }
    }

    private AppointmentEventConsumer createEventWithId(UUID eventId) {
        AppointmentEventConsumer event = new AppointmentEventConsumer();
        event.setEventId(eventId.toString());
        event.setAppointmentId(UUID.randomUUID().toString());
        event.setPatientId(UUID.randomUUID().toString());
        event.setDoctorId(UUID.randomUUID().toString());
        event.setDoctorName("Dr. Test");
        event.setPatientName("Patient Test");
        event.setPatientEmail("test@test.com");
        event.setStatus("CONFIRMED");
        event.setEventType("CREATED");
        event.setTimestamp("2025-12-08T10:00:00Z");
        event.setAppointmentDate("2025-12-08T10:00:00-03:00");
        return event;
    }
}


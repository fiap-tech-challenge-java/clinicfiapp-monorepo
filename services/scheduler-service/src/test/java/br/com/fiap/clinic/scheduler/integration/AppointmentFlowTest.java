package br.com.fiap.clinic.scheduler.integration;

import br.com.fiap.clinic.scheduler.AbstractIntegrationTest;
import br.com.fiap.clinic.scheduler.domain.entity.Appointment;
import br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus;
import br.com.fiap.clinic.scheduler.domain.entity.User;
import br.com.fiap.clinic.scheduler.domain.repository.AppointmentRepository;
import br.com.fiap.clinic.scheduler.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes de Integração - Fluxo de Agendamento")
class AppointmentFlowTest extends AbstractIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private String patientId;
    private String doctorId;

    @BeforeEach
    void setUp() {
        // Começa logado como enfermeiro para preparar os dados
        authenticateAs("enfermeiro");
        this.patientId = criarPaciente();
        this.doctorId = criarMedico();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username) {
        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Deve agendar uma consulta com sucesso")
    void deveAgendarConsulta() {
        String startAt = OffsetDateTime.now().plusDays(1).withHour(14).withMinute(0).withNano(0).toString();
        String endAt = OffsetDateTime.now().plusDays(1).withHour(15).withMinute(0).withNano(0).toString();

        String mutation = """
            mutation {
                createAppointment(input: {
                    patientId: "%s"
                    doctorId: "%s"
                    startAt: "%s"
                    endAt: "%s"
                }) {
                    id
                    status
                    patient { name }
                    doctor { name }
                }
            }
            """.formatted(patientId, doctorId, startAt, endAt);

        GraphQlTester.Response response = graphQlTester.document(mutation).execute();

        response.path("createAppointment.status").entity(String.class).isEqualTo("SCHEDULED")
                .path("createAppointment.patient.name").entity(String.class).isEqualTo("Pacient Flow Test")
                .path("createAppointment.doctor.name").entity(String.class).isEqualTo("Doctor Flow Test");
    }

    @Test
    @DisplayName("Deve confirmar uma consulta agendada")
    void deveConfirmarConsulta() {
        // 1. Cria a consulta (como enfermeiro)
        String appointmentId = criarConsultaAgendada();

        // 2. Confirma
        String mutation = """
            mutation {
                confirmAppointment(id: "%s") {
                    id
                    status
                }
            }
            """.formatted(appointmentId);

        graphQlTester.document(mutation).execute()
                .path("confirmAppointment.status").entity(String.class).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("Deve permitir que médico finalize a consulta confirmada")
    void deveFinalizarConsultaPeloMedico() {
        // 1. Setup: Cria consulta e confirma (como enfermeiro)
        String appointmentId = criarConsultaAgendada();
        confirmarConsulta(appointmentId);

        // 2. Troca contexto para MÉDICO (apenas médico pode finalizar)
        authenticateAs("medico");

        // 3. Finaliza
        String mutation = """
            mutation {
                completeAppointment(id: "%s") {
                    id
                    status
                }
            }
            """.formatted(appointmentId);

        graphQlTester.document(mutation).execute()
                .path("completeAppointment.status").entity(String.class).isEqualTo("COMPLETED");

        // Valida banco
        Appointment apt = appointmentRepository.findById(UUID.fromString(appointmentId)).orElseThrow();
        assertThat(apt.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Deve reagendar uma consulta")
    void deveReagendarConsulta() {
        String appointmentId = criarConsultaAgendada();

           OffsetDateTime safeBusinessDay = OffsetDateTime.now()
                .with(TemporalAdjusters.next(DayOfWeek.TUESDAY));

        String newStart = safeBusinessDay.withHour(10).withMinute(0).withNano(0).toString();
        String newEnd = safeBusinessDay.withHour(11).withMinute(0).withNano(0).toString();

        String mutation = """
            mutation {
                rescheduleAppointment(input: {
                    appointmentId: "%s"
                    newStartAt: "%s"
                    newEndAt: "%s"
                }) {
                    id
                    status
                    startAt
                }
            }
            """.formatted(appointmentId, newStart, newEnd);

        graphQlTester.document(mutation).execute()
                .path("rescheduleAppointment.status").entity(String.class).isEqualTo("RESCHEDULED");
    }

    // --- Helpers de Teste ---

    private String criarConsultaAgendada() {
        String startAt = OffsetDateTime.now().plusDays(1).withHour(10).withNano(0).atZoneSameInstant(ZoneOffset.UTC).toString();
        String endAt = OffsetDateTime.now().plusDays(1).withHour(11).withNano(0).atZoneSameInstant(ZoneOffset.UTC).toString();

        String mutation = """
            mutation {
                createAppointment(input: {
                    patientId: "%s"
                    doctorId: "%s"
                    startAt: "%s"
                    endAt: "%s"
                }) { id }
            }
            """.formatted(patientId, doctorId, startAt, endAt);

        return graphQlTester.document(mutation).execute()
                .path("createAppointment.id").entity(String.class).get();
    }

    private void confirmarConsulta(String id) {
        String mutation = """
            mutation { confirmAppointment(id: "%s") { id } }
            """.formatted(id);
        graphQlTester.document(mutation).execute();
    }

    private String criarPaciente() {
        String unique = "p" + System.currentTimeMillis();
        String mutation = """
            mutation {
                createPatient(input: {
                    name: "Pacient Flow Test"
                    email: "%s@flow.com"
                    login: "%s"
                    password: "123"
                }) { id }
            }
            """.formatted(unique, unique);
        return graphQlTester.document(mutation).execute().path("createPatient.id").entity(String.class).get();
    }

    private String criarMedico() {
        String unique = "d" + System.currentTimeMillis();
        String mutation = """
            mutation {
                createDoctor(input: {
                    name: "Doctor Flow Test"
                    email: "%s@flow.com"
                    login: "%s"
                    password: "123"
                    crm: "CRM %s"
                    specialty: "Geral"
                }) { id }
            }
            """.formatted(unique, unique, unique);
        return graphQlTester.document(mutation).execute().path("createDoctor.id").entity(String.class).get();
    }
}
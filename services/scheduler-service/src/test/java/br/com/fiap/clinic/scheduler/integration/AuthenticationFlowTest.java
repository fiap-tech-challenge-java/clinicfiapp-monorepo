package br.com.fiap.clinic.scheduler.integration;

import br.com.fiap.clinic.scheduler.AbstractIntegrationTest;
import br.com.fiap.clinic.scheduler.domain.entity.*;
import br.com.fiap.clinic.scheduler.domain.repository.DoctorRepository;
import br.com.fiap.clinic.scheduler.domain.repository.NurseRepository;
import br.com.fiap.clinic.scheduler.domain.repository.PatientRepository;
import br.com.fiap.clinic.scheduler.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes de Integração - Autenticação e Autorização")
class AuthenticationFlowTest extends AbstractIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NurseRepository nurseRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve realizar login com sucesso para enfermeiro")
    void deveRealizarLoginEnfermeiroComSucesso() {
        // Arrange - Cria usuário dinamicamente
        String login = "nurse_auth_" + System.currentTimeMillis();
        String password = "123";
        criarEnfermeiro(login, password);

        String mutation = """
            mutation {
                login(input: {
                    login: "%s"
                    password: "%s"
                }) {
                    token
                    type
                }
            }
            """.formatted(login, password);

        // Act
        GraphQlTester.Response response = graphQlTester.document(mutation).execute();

        // Assert
        String token = response.path("login.token").entity(String.class).get();
        String type = response.path("login.type").entity(String.class).get();

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(type).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Deve realizar login com sucesso para médico")
    void deveRealizarLoginMedicoComSucesso() {
        // Arrange
        String login = "doc_auth_" + System.currentTimeMillis();
        String password = "123";
        criarMedico(login, password);

        String mutation = """
            mutation {
                login(input: {
                    login: "%s"
                    password: "%s"
                }) {
                    token
                    type
                }
            }
            """.formatted(login, password);

        // Act
        GraphQlTester.Response response = graphQlTester.document(mutation).execute();

        // Assert
        assertThat(response.path("login.token").entity(String.class).get()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve realizar login com sucesso para paciente")
    void deveRealizarLoginPacienteComSucesso() {
        // Arrange
        String login = "paciente_auth_" + System.currentTimeMillis();
        String password = "123";
        criarPaciente(login, password);

        String mutation = """
            mutation {
                login(input: {
                    login: "%s"
                    password: "%s"
                }) {
                    token
                    type
                }
            }
            """.formatted(login, password);

        // Act
        GraphQlTester.Response response = graphQlTester.document(mutation).execute();

        // Assert
        assertThat(response.path("login.token").entity(String.class).get()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve retornar erro ao tentar login com credenciais inválidas")
    void deveRetornarErroParaCredenciaisInvalidas() {
        String mutation = """
            mutation {
                login(input: {
                    login: "inexistente"
                    password: "qualquercoisa"
                }) { token }
            }
            """;

        graphQlTester.document(mutation)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    @DisplayName("Deve retornar erro ao tentar login com senha incorreta")
    void deveRetornarErroParaSenhaIncorreta() {
        // Arrange
        String login = "nurse_wrong_" + System.currentTimeMillis();
        criarEnfermeiro(login, "senhaCorreta");

        String mutation = """
            mutation {
                login(input: {
                    login: "%s"
                    password: "senhaErrada"
                }) { token }
            }
            """.formatted(login);

        // Act & Assert
        graphQlTester.document(mutation)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    @DisplayName("Deve retornar erro ao acessar recurso protegido sem autenticação")
    void deveRetornarErroSemAutenticacao() {
        String query = """
            query {
                appointments { id }
            }
            """;

        graphQlTester.document(query)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    @DisplayName("Deve permitir acesso com autenticação válida")
    void devePermitirAcessoComAutenticacaoValida() {
        // Arrange
        String login = "nurse_access_" + System.currentTimeMillis();
        criarEnfermeiro(login, "123");
        authenticateAs(login);

        String query = """
            query {
                appointments { id }
            }
            """;

        graphQlTester.document(query)
                .execute()
                .path("appointments").hasValue();
    }

    @Test
    @DisplayName("Deve retornar erro ao tentar acessar recurso sem permissão")
    void deveRetornarErroSemPermissao() {
        // Arrange
        // 1. Cria um Paciente e autentica com ele
        String patientLogin = "paciente_sem_perm_" + System.currentTimeMillis();
        Patient patient = criarPaciente(patientLogin, "123");
        authenticateAs(patientLogin);

        // 2. Cria um Médico (necessário para o ID no input)
        Doctor doctor = criarMedico("doc_target_" + System.currentTimeMillis(), "123");

        // 3. Tenta criar agendamento (Ação permitida apenas para Nurse/Doctor)
        String mutation = """
            mutation {
                createAppointment(input: {
                    patientId: "%s"
                    doctorId: "%s"
                    startAt: "2025-12-10T10:00:00-03:00"
                    endAt: "2025-12-10T11:00:00-03:00"
                }) { id }
            }
            """.formatted(patient.getId(), doctor.getId());

        // Act & Assert
        graphQlTester.document(mutation)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                    // Opcional: verificar mensagem específica de Access Denied
                    assertThat(errors.get(0).getMessage()).contains("Acesso negado");
                });
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private void authenticateAs(String username) {
        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Nurse criarEnfermeiro(String login, String rawPassword) {
        Nurse nurse = new Nurse();
        nurse.setName("Nurse Test");
        nurse.setEmail(login + "@test.com");
        nurse.setLogin(login);
        nurse.setPassword(passwordEncoder.encode(rawPassword));
        nurse.setRole(Role.nurse);
        return nurseRepository.save(nurse);
    }

    private Doctor criarMedico(String login, String rawPassword) {
        Doctor doctor = new Doctor();
        doctor.setName("Doctor Test");
        doctor.setEmail(login + "@test.com");
        doctor.setLogin(login);
        doctor.setPassword(passwordEncoder.encode(rawPassword));
        doctor.setRole(Role.doctor);
        doctor.setCrm("CRM-" + login); // Garante unicidade
        doctor.setSpecialty("General");
        return doctorRepository.save(doctor);
    }

    private Patient criarPaciente(String login, String rawPassword) {
        Patient patient = new Patient();
        patient.setName("Patient Test");
        patient.setEmail(login + "@test.com");
        patient.setLogin(login);
        patient.setPassword(passwordEncoder.encode(rawPassword));
        patient.setRole(Role.patient);
        return patientRepository.save(patient);
    }
}
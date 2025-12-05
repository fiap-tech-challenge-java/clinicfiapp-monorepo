package br.com.fiap.clinic.scheduler.integration;

import br.com.fiap.clinic.scheduler.AbstractIntegrationTest;
import br.com.fiap.clinic.scheduler.domain.entity.Patient;
import br.com.fiap.clinic.scheduler.domain.entity.User;
import br.com.fiap.clinic.scheduler.domain.repository.PatientRepository;
import br.com.fiap.clinic.scheduler.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes de Integração - Gerenciamento de Pacientes")
class PatientManagementFlowTest extends AbstractIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        authenticateAs("enfermeiro");
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
    @DisplayName("Deve criar um novo paciente")
    void deveCriarNovoPaciente() {
        // Arrange
        long countBefore = patientRepository.count();
        String uniqueEmail = "patient" + System.currentTimeMillis() + "@example.com";
        String uniqueLogin = "patient" + System.currentTimeMillis();

        String mutation = """
            mutation {
                createPatient(input: {
                    name: "João Silva"
                    email: "%s"
                    login: "%s"
                    password: "senha123"
                    birthDate: "1985-05-15"
                }) {
                    id
                    name
                    email
                    login
                    birthDate
                    isActive
                }
            }
            """.formatted(uniqueEmail, uniqueLogin);

        // Act
        GraphQlTester.Response response = graphQlTester.document(mutation)
                .execute();

        // Assert
        response.path("createPatient.name").entity(String.class).isEqualTo("João Silva")
                .path("createPatient.email").entity(String.class).isEqualTo(uniqueEmail)
                .path("createPatient.login").entity(String.class).isEqualTo(uniqueLogin)
                .path("createPatient.birthDate").entity(String.class).isEqualTo("1985-05-15")
                .path("createPatient.isActive").entity(Boolean.class).isEqualTo(true);

        assertThat(patientRepository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    @DisplayName("Deve atualizar informações de um paciente")
    void deveAtualizarPaciente() {
        // Arrange
        String patientId = criarPacienteTeste();

        String mutation = """
            mutation {
                updatePatient(id: "%s", input: {
                    name: "João Silva Atualizado"
                    email: "joao.novo@example.com"
                }) {
                    id
                    name
                    email
                }
            }
            """.formatted(patientId);

        // Act
        GraphQlTester.Response response = graphQlTester.document(mutation)
                .execute();

        // Assert
        response.path("updatePatient.name").entity(String.class).isEqualTo("João Silva Atualizado")
                .path("updatePatient.email").entity(String.class).isEqualTo("joao.novo@example.com");
    }

    @Test
    @DisplayName("Deve desativar um paciente")
    void deveDesativarPaciente() {
        // Arrange
        String patientId = criarPacienteTeste();

        String mutation = """
            mutation {
                deactivatePatient(id: "%s")
            }
            """.formatted(patientId);

        // Act
        GraphQlTester.Response response = graphQlTester.document(mutation)
                .execute();

        // Assert
        response.path("deactivatePatient").entity(Boolean.class).isEqualTo(true);

        Patient patient = patientRepository.findById(java.util.UUID.fromString(patientId)).orElseThrow();
        assertThat(patient.isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve listar todos os pacientes")
    void deveListarTodosPacientes() {
        // Arrange
        criarPacienteTeste();

        String query = """
            query {
                patients {
                    id
                    name
                    email
                    isActive
                }
            }
            """;

        // Act
        GraphQlTester.Response response = graphQlTester.document(query)
                .execute();

        // Assert
        response.path("patients").entityList(Object.class).hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("Deve buscar um paciente específico por ID")
    void deveBuscarPacientePorId() {
        // Arrange
        String patientId = criarPacienteTeste();

        String query = """
            query {
                patient(id: "%s") {
                    id
                    name
                    email
                }
            }
            """.formatted(patientId);

        // Act
        GraphQlTester.Response response = graphQlTester.document(query)
                .execute();

        // Assert
        response.path("patient.id").entity(String.class).isEqualTo(patientId);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private String criarPacienteTeste() {
        String uniqueEmail = "patient" + System.currentTimeMillis() + "@example.com";
        String uniqueLogin = "patient" + System.currentTimeMillis();

        String mutation = """
            mutation {
                createPatient(input: {
                    name: "Paciente Teste"
                    email: "%s"
                    login: "%s"
                    password: "senha123"
                    birthDate: "1990-01-01"
                }) {
                    id
                }
            }
            """.formatted(uniqueEmail, uniqueLogin);

        return graphQlTester.document(mutation)
                .execute()
                .path("createPatient.id")
                .entity(String.class)
                .get();
    }
}


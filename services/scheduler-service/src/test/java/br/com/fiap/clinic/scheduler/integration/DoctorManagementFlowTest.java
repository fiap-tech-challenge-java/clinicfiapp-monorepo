package br.com.fiap.clinic.scheduler.integration;

import br.com.fiap.clinic.scheduler.AbstractIntegrationTest;
import br.com.fiap.clinic.scheduler.domain.entity.Doctor;
import br.com.fiap.clinic.scheduler.domain.entity.User;
import br.com.fiap.clinic.scheduler.domain.repository.DoctorRepository;
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

@DisplayName("Testes de Integração - Gerenciamento de Médicos")
class DoctorManagementFlowTest extends AbstractIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

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
    @DisplayName("Deve criar um novo médico")
    void deveCriarNovoMedico() {
        // Arrange
        long countBefore = doctorRepository.count();
        String uniqueCrm = "CRM/SP " + System.currentTimeMillis();
        String uniqueEmail = "doctor" + System.currentTimeMillis() + "@clinic.com";
        String uniqueLogin = "doctor" + System.currentTimeMillis();

        String mutation = """
            mutation {
                createDoctor(input: {
                    name: "Dr. Maria Santos"
                    email: "%s"
                    login: "%s"
                    password: "senha123"
                    crm: "%s"
                    specialty: "Cardiologia"
                }) {
                    id
                    name
                    email
                    crm
                    specialty
                    isActive
                }
            }
            """.formatted(uniqueEmail, uniqueLogin, uniqueCrm);

        // Act
        GraphQlTester.Response response = graphQlTester.document(mutation)
                .execute();

        // Assert
        response.path("createDoctor.name").entity(String.class).isEqualTo("Dr. Maria Santos")
                .path("createDoctor.crm").entity(String.class).isEqualTo(uniqueCrm)
                .path("createDoctor.specialty").entity(String.class).isEqualTo("Cardiologia")
                .path("createDoctor.isActive").entity(Boolean.class).isEqualTo(true);

        assertThat(doctorRepository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    @DisplayName("Deve atualizar informações de um médico")
    void deveAtualizarMedico() {
        // Arrange
        String doctorId = criarMedicoTeste();

        String mutation = """
            mutation {
                updateDoctor(id: "%s", input: {
                    name: "Dr. Maria Santos Atualizado"
                    specialty: "Cardiologia Pediátrica"
                }) {
                    id
                    name
                    specialty
                }
            }
            """.formatted(doctorId);

        // Act
        GraphQlTester.Response response = graphQlTester.document(mutation)
                .execute();

        // Assert
        response.path("updateDoctor.name").entity(String.class).isEqualTo("Dr. Maria Santos Atualizado")
                .path("updateDoctor.specialty").entity(String.class).isEqualTo("Cardiologia Pediátrica");
    }

    @Test
    @DisplayName("Deve desativar um médico")
    void deveDesativarMedico() {
        // Arrange
        String doctorId = criarMedicoTeste();

        String mutation = """
            mutation {
                deactivateDoctor(id: "%s")
            }
            """.formatted(doctorId);

        // Act
        GraphQlTester.Response response = graphQlTester.document(mutation)
                .execute();

        // Assert
        response.path("deactivateDoctor").entity(Boolean.class).isEqualTo(true);

        Doctor doctor = doctorRepository.findById(java.util.UUID.fromString(doctorId)).orElseThrow();
        assertThat(doctor.isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve listar todos os médicos")
    void deveListarTodosMedicos() {
        // Arrange
        criarMedicoTeste();

        String query = """
            query {
                doctors {
                    id
                    name
                    crm
                    specialty
                    isActive
                }
            }
            """;

        // Act
        GraphQlTester.Response response = graphQlTester.document(query)
                .execute();

        // Assert
        response.path("doctors").entityList(Object.class).hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("Deve buscar um médico específico por ID")
    void deveBuscarMedicoPorId() {
        // Arrange
        String doctorId = criarMedicoTeste();

        String query = """
            query {
                doctor(id: "%s") {
                    id
                    name
                    crm
                    specialty
                }
            }
            """.formatted(doctorId);

        // Act
        GraphQlTester.Response response = graphQlTester.document(query)
                .execute();

        // Assert
        response.path("doctor.id").entity(String.class).isEqualTo(doctorId);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private String criarMedicoTeste() {
        String uniqueEmail = "doctor" + System.currentTimeMillis() + "@clinic.com";
        String uniqueLogin = "doctor" + System.currentTimeMillis();
        String uniqueCrm = "CRM/SP " + System.currentTimeMillis();

        String mutation = """
            mutation {
                createDoctor(input: {
                    name: "Dr. Teste"
                    email: "%s"
                    login: "%s"
                    password: "senha123"
                    crm: "%s"
                    specialty: "Clínica Geral"
                }) {
                    id
                }
            }
            """.formatted(uniqueEmail, uniqueLogin, uniqueCrm);

        return graphQlTester.document(mutation)
                .execute()
                .path("createDoctor.id")
                .entity(String.class)
                .get();
    }
}


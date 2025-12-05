package br.com.fiap.clinic.scheduler.integration;

import br.com.fiap.clinic.scheduler.AbstractIntegrationTest;
import br.com.fiap.clinic.scheduler.domain.entity.Nurse;
import br.com.fiap.clinic.scheduler.domain.entity.User;
import br.com.fiap.clinic.scheduler.domain.repository.NurseRepository;
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

@DisplayName("Testes de Integração - Gerenciamento de Enfermeiros")
class NurseManagementFlowTest extends AbstractIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NurseRepository nurseRepository;

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
    @DisplayName("Deve criar um novo enfermeiro")
    void deveCriarNovoEnfermeiro() {
        long countBefore = nurseRepository.count();
        String uniqueLogin = "nurse" + System.currentTimeMillis();

        String mutation = """
            mutation {
                createNurse(input: {
                    name: "Enfermeira Joy"
                    email: "%s@pokemon.com"
                    login: "%s"
                    password: "123"
                }) {
                    id
                    name
                    isActive
                }
            }
            """.formatted(uniqueLogin, uniqueLogin);

        GraphQlTester.Response response = graphQlTester.document(mutation).execute();

        response.path("createNurse.name").entity(String.class).isEqualTo("Enfermeira Joy")
                .path("createNurse.isActive").entity(Boolean.class).isEqualTo(true);

        assertThat(nurseRepository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    @DisplayName("Deve desativar um enfermeiro")
    void deveDesativarEnfermeiro() {
        String nurseId = criarEnfermeiroTeste();

        String mutation = """
            mutation {
                deactivateNurse(id: "%s")
            }
            """.formatted(nurseId);

        graphQlTester.document(mutation).execute()
                .path("deactivateNurse").entity(Boolean.class).isEqualTo(true);

        Nurse nurse = nurseRepository.findById(java.util.UUID.fromString(nurseId)).orElseThrow();
        assertThat(nurse.isActive()).isFalse();
    }

    private String criarEnfermeiroTeste() {
        String uniqueLogin = "nurse_test_" + System.currentTimeMillis();

        String mutation = """
            mutation {
                createNurse(input: {
                    name: "Enfermeiro Teste"
                    email: "%s@test.com"
                    login: "%s"
                    password: "123"
                }) { id }
            }
            """.formatted(uniqueLogin, uniqueLogin);

        return graphQlTester.document(mutation)
                .execute()
                .path("createNurse.id")
                .entity(String.class)
                .get();
    }
}
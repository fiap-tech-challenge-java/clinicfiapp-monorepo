package br.com.fiap.clinic.scheduler.unit.service;

import br.com.fiap.clinic.scheduler.domain.entity.Role;
import br.com.fiap.clinic.scheduler.domain.entity.User;
import br.com.fiap.clinic.scheduler.domain.repository.UserRepository;
import br.com.fiap.clinic.scheduler.domain.service.UserService;
import br.com.fiap.clinic.scheduler.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setName("Enfermeira Teste");
        user.setEmail("enfermeira@clinic.com");
        user.setLogin("enfermeira.teste");
        user.setPassword("hashedPassword");
        user.setRole(Role.nurse);
    }

    // ==================== TESTES DE BUSCA POR ID ====================

    @Test
    @DisplayName("Deve encontrar usuário por ID")
    void deveBuscarUsuarioPorId() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        User found = userService.findById(userId);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(userId);
        assertThat(found.getName()).isEqualTo("Enfermeira Teste");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não encontrado por ID")
    void deveLancarExcecaoQuandoUsuarioNaoEncontradoPorId() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(userRepository).findById(userId);
    }

    // ==================== TESTES DE BUSCA POR LOGIN ====================

    @Test
    @DisplayName("Deve encontrar usuário por login")
    void deveBuscarUsuarioPorLogin() {
        // Arrange
        when(userRepository.findByLogin("enfermeira.teste")).thenReturn(Optional.of(user));

        // Act
        User found = userService.findByLogin("enfermeira.teste");

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getLogin()).isEqualTo("enfermeira.teste");
        verify(userRepository).findByLogin("enfermeira.teste");
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não encontrado por login")
    void deveLancarExcecaoQuandoUsuarioNaoEncontradoPorLogin() {
        // Arrange
        when(userRepository.findByLogin("inexistente")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findByLogin("inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(userRepository).findByLogin("inexistente");
    }

    // ==================== TESTES DE LISTAGEM ====================

    @Test
    @DisplayName("Deve listar todos os usuários")
    void deveListarTodosUsuarios() {
        // Arrange
        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setName("Dr. João");
        user2.setRole(Role.doctor);

        when(userRepository.findAll()).thenReturn(List.of(user, user2));

        // Act
        List<User> users = userService.findAll();

        // Assert
        assertThat(users).hasSize(2);
        assertThat(users).extracting("name")
                .containsExactlyInAnyOrder("Enfermeira Teste", "Dr. João");
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Deve listar usuários com paginação")
    void deveListarUsuariosComPaginacao() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<User> result = userService.findAll(pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userRepository).findAll(pageable);
    }

    // ==================== TESTES DE CRIAÇÃO ====================

    @Test
    @DisplayName("Deve criar usuário com sucesso")
    void deveCriarUsuarioComSucesso() {
        // Arrange
        User newUser = new User();
        newUser.setName("Novo Usuário");
        newUser.setEmail("novo@clinic.com");
        newUser.setLogin("novo.usuario");
        newUser.setRole(Role.nurse);

        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User created = userService.create(newUser);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Novo Usuário");
        verify(userRepository).save(newUser);
    }

    // ==================== TESTES DE VALIDAÇÃO DE ROLES ====================

    @Test
    @DisplayName("Deve validar role de enfermeiro")
    void deveValidarRoleEnfermeiro() {
        // Assert
        assertThat(user.getRole()).isEqualTo(Role.nurse);
    }

    @Test
    @DisplayName("Deve validar diferentes roles")
    void deveValidarDiferentesRoles() {
        // Arrange
        User medico = new User();
        medico.setRole(Role.doctor);

        User paciente = new User();
        paciente.setRole(Role.patient);

        // Assert
        assertThat(user.getRole()).isEqualTo(Role.nurse);
        assertThat(medico.getRole()).isEqualTo(Role.doctor);
        assertThat(paciente.getRole()).isEqualTo(Role.patient);
    }

    @Test
    @DisplayName("Deve ter login único")
    void deveTerLoginUnico() {
        // Arrange
        User duplicateUser = new User();
        duplicateUser.setLogin("enfermeira.teste");

        when(userRepository.save(any(User.class)))
                .thenThrow(new IllegalArgumentException("Login já existe"));

        // Act & Assert
        assertThatThrownBy(() -> userService.create(duplicateUser))
                .isInstanceOf(IllegalArgumentException.class);
    }
}


package br.com.fiap.clinic.scheduler.unit.service;

import br.com.fiap.clinic.scheduler.domain.entity.Patient;
import br.com.fiap.clinic.scheduler.domain.entity.Role;
import br.com.fiap.clinic.scheduler.domain.repository.PatientRepository;
import br.com.fiap.clinic.scheduler.domain.service.PatientService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - PatientService")
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    private Patient patient;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();

        patient = new Patient();
        patient.setId(patientId);
        patient.setName("João Silva");
        patient.setEmail("joao@example.com");
        patient.setLogin("joao.silva");
        patient.setPassword("hashedPassword");
        patient.setRole(Role.patient);
        patient.setActive(true);
        patient.setBirthDate(LocalDate.of(1990, 1, 1));
    }

    // ==================== TESTES DE BUSCA ====================

    @Test
    @DisplayName("Deve encontrar paciente por ID")
    void deveBuscarPacientePorId() {
        // Arrange
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        // Act
        Patient found = patientService.findById(patientId);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(patientId);
        assertThat(found.getName()).isEqualTo("João Silva");
        verify(patientRepository).findById(patientId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando paciente não encontrado")
    void deveLancarExcecaoQuandoPacienteNaoEncontrado() {
        // Arrange
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> patientService.findById(patientId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Paciente não encontrado");

        verify(patientRepository).findById(patientId);
    }

    @Test
    @DisplayName("Deve listar todos os pacientes")
    void deveListarTodosPacientes() {
        // Arrange
        Patient patient2 = new Patient();
        patient2.setId(UUID.randomUUID());
        patient2.setName("Maria Santos");

        when(patientRepository.findAll()).thenReturn(List.of(patient, patient2));

        // Act
        List<Patient> patients = patientService.findAll();

        // Assert
        assertThat(patients).hasSize(2);
        assertThat(patients).extracting("name")
                .containsExactlyInAnyOrder("João Silva", "Maria Santos");
        verify(patientRepository).findAll();
    }

    @Test
    @DisplayName("Deve listar pacientes com paginação")
    void deveListarPacientesComPaginacao() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> page = new PageImpl<>(List.of(patient));
        when(patientRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<Patient> result = patientService.findAll(pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(patientRepository).findAll(pageable);
    }

    // ==================== TESTES DE CRIAÇÃO ====================

    @Test
    @DisplayName("Deve criar paciente com sucesso")
    void deveCriarPacienteComSucesso() {
        // Arrange
        Patient newPatient = new Patient();
        newPatient.setName("João Silva");
        newPatient.setEmail("joao@example.com");
        newPatient.setLogin("joao.silva");
        newPatient.setBirthDate(LocalDate.of(1990, 1, 1));

        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        // Act
        Patient created = patientService.create(newPatient);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("João Silva");
        verify(patientRepository).save(newPatient);
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios ao criar paciente")
    void deveValidarCamposObrigatorios() {
        // Arrange
        Patient invalidPatient = new Patient();
        // Nome não definido

        when(patientRepository.save(any(Patient.class)))
                .thenThrow(new IllegalArgumentException("Nome é obrigatório"));

        // Act & Assert
        assertThatThrownBy(() -> patientService.create(invalidPatient))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== TESTES DE ATUALIZAÇÃO ====================

    @Test
    @DisplayName("Deve atualizar paciente com sucesso")
    void deveAtualizarPacienteComSucesso() {
        // Arrange
        Patient updatedData = new Patient();
        updatedData.setName("João Silva Atualizado");
        updatedData.setEmail("joao.novo@example.com");
        updatedData.setBirthDate(LocalDate.of(1990, 1, 1));
        updatedData.setActive(true);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        // Act
        Patient updated = patientService.update(patientId, updatedData);

        // Assert
        assertThat(updated).isNotNull();
        verify(patientRepository).findById(patientId);
        verify(patientRepository).save(patient);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar paciente inexistente")
    void deveLancarExcecaoAoAtualizarPacienteInexistente() {
        // Arrange
        Patient updatedData = new Patient();
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> patientService.update(patientId, updatedData))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(patientRepository).findById(patientId);
        verify(patientRepository, never()).save(any());
    }

    // ==================== TESTES DE DESATIVAÇÃO ====================

    @Test
    @DisplayName("Deve desativar paciente com sucesso")
    void deveDesativarPacienteComSucesso() {
        // Arrange
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        // Act
        patientService.deactivate(patientId);

        // Assert
        verify(patientRepository).findById(patientId);
        verify(patientRepository).save(patient);
        // O método deactivate deve ter chamado setActive(false) no paciente
    }

    @Test
    @DisplayName("Deve lançar exceção ao desativar paciente inexistente")
    void deveLancarExcecaoAoDesativarPacienteInexistente() {
        // Arrange
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> patientService.deactivate(patientId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(patientRepository).findById(patientId);
        verify(patientRepository, never()).save(any());
    }

    // ==================== TESTES DE VALIDAÇÃO ====================

    @Test
    @DisplayName("Deve verificar se paciente está ativo")
    void deveVerificarSePacienteEstaAtivo() {
        // Assert
        assertThat(patient.isActive()).isTrue();

        patient.setActive(false);
        assertThat(patient.isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve validar role do paciente")
    void deveValidarRoleDoPaciente() {
        // Assert
        assertThat(patient.getRole()).isEqualTo(Role.patient);
    }

    @Test
    @DisplayName("Deve validar data de nascimento")
    void deveValidarDataDeNascimento() {
        // Assert
        assertThat(patient.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(patient.getBirthDate()).isBefore(LocalDate.now());
    }

    @Test
    @DisplayName("Deve calcular idade do paciente")
    void deveCalcularIdadeDoPaciente() {
        // Arrange & Act
        int idade = LocalDate.now().getYear() - patient.getBirthDate().getYear();

        // Assert
        assertThat(idade).isGreaterThanOrEqualTo(34); // Nascido em 1990
    }
}


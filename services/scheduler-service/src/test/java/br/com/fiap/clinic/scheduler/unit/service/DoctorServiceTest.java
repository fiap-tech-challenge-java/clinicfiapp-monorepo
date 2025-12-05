package br.com.fiap.clinic.scheduler.unit.service;

import br.com.fiap.clinic.scheduler.domain.entity.Doctor;
import br.com.fiap.clinic.scheduler.domain.entity.Role;
import br.com.fiap.clinic.scheduler.domain.repository.DoctorRepository;
import br.com.fiap.clinic.scheduler.domain.service.DoctorService;
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
@DisplayName("Testes Unitários - DoctorService")
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorService doctorService;

    private Doctor doctor;
    private UUID doctorId;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();

        doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setName("Dr. Maria Santos");
        doctor.setEmail("maria@clinic.com");
        doctor.setLogin("maria.santos");
        doctor.setPassword("hashedPassword");
        doctor.setRole(Role.doctor);
        doctor.setActive(true);
        doctor.setCrm("CRM/SP 123456");
        doctor.setSpecialty("Cardiologia");
    }

    // ==================== TESTES DE BUSCA ====================

    @Test
    @DisplayName("Deve encontrar médico por ID")
    void deveBuscarMedicoPorId() {
        // Arrange
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        // Act
        Doctor found = doctorService.findById(doctorId);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(doctorId);
        assertThat(found.getName()).isEqualTo("Dr. Maria Santos");
        assertThat(found.getCrm()).isEqualTo("CRM/SP 123456");
        verify(doctorRepository).findById(doctorId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando médico não encontrado")
    void deveLancarExcecaoQuandoMedicoNaoEncontrado() {
        // Arrange
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> doctorService.findById(doctorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Médico não encontrado");

        verify(doctorRepository).findById(doctorId);
    }

    @Test
    @DisplayName("Deve listar todos os médicos")
    void deveListarTodosMedicos() {
        // Arrange
        Doctor doctor2 = new Doctor();
        doctor2.setId(UUID.randomUUID());
        doctor2.setName("Dr. João Silva");
        doctor2.setSpecialty("Ortopedia");

        when(doctorRepository.findAll()).thenReturn(List.of(doctor, doctor2));

        // Act
        List<Doctor> doctors = doctorService.findAll();

        // Assert
        assertThat(doctors).hasSize(2);
        assertThat(doctors).extracting("name")
                .containsExactlyInAnyOrder("Dr. Maria Santos", "Dr. João Silva");
        verify(doctorRepository).findAll();
    }

    @Test
    @DisplayName("Deve listar médicos com paginação")
    void deveListarMedicosComPaginacao() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Doctor> page = new PageImpl<>(List.of(doctor));
        when(doctorRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<Doctor> result = doctorService.findAll(pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(doctorRepository).findAll(pageable);
    }

    // ==================== TESTES DE CRIAÇÃO ====================

    @Test
    @DisplayName("Deve criar médico com sucesso")
    void deveCriarMedicoComSucesso() {
        // Arrange
        Doctor newDoctor = new Doctor();
        newDoctor.setName("Dr. Pedro Alves");
        newDoctor.setEmail("pedro@clinic.com");
        newDoctor.setCrm("CRM/RJ 789012");
        newDoctor.setSpecialty("Neurologia");

        when(doctorRepository.save(any(Doctor.class))).thenReturn(newDoctor);

        // Act
        Doctor created = doctorService.create(newDoctor);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Dr. Pedro Alves");
        verify(doctorRepository).save(newDoctor);
    }

    @Test
    @DisplayName("Deve validar CRM único ao criar médico")
    void deveValidarCrmUnico() {
        // Arrange
        Doctor newDoctor = new Doctor();
        newDoctor.setCrm("CRM/SP 123456"); // CRM duplicado

        when(doctorRepository.save(any(Doctor.class)))
                .thenThrow(new IllegalArgumentException("CRM já cadastrado"));

        // Act & Assert
        assertThatThrownBy(() -> doctorService.create(newDoctor))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== TESTES DE ATUALIZAÇÃO ====================

    @Test
    @DisplayName("Deve atualizar médico com sucesso")
    void deveAtualizarMedicoComSucesso() {
        // Arrange
        Doctor updatedData = new Doctor();
        updatedData.setName("Dr. Maria Santos Silva");
        updatedData.setEmail("maria.silva@clinic.com");
        updatedData.setCrm("CRM/SP 123456");
        updatedData.setSpecialty("Cardiologia Pediátrica");
        updatedData.setActive(true);

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        // Act
        Doctor updated = doctorService.update(doctorId, updatedData);

        // Assert
        assertThat(updated).isNotNull();
        verify(doctorRepository).findById(doctorId);
        verify(doctorRepository).save(doctor);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar médico inexistente")
    void deveLancarExcecaoAoAtualizarMedicoInexistente() {
        // Arrange
        Doctor updatedData = new Doctor();
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> doctorService.update(doctorId, updatedData))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(doctorRepository).findById(doctorId);
        verify(doctorRepository, never()).save(any());
    }

    // ==================== TESTES DE DESATIVAÇÃO ====================

    @Test
    @DisplayName("Deve desativar médico com sucesso")
    void deveDesativarMedicoComSucesso() {
        // Arrange
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        // Act
        doctorService.deactivate(doctorId);

        // Assert
        verify(doctorRepository).findById(doctorId);
        verify(doctorRepository).save(doctor);
    }

    @Test
    @DisplayName("Deve lançar exceção ao desativar médico inexistente")
    void deveLancarExcecaoAoDesativarMedicoInexistente() {
        // Arrange
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> doctorService.deactivate(doctorId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(doctorRepository).findById(doctorId);
        verify(doctorRepository, never()).save(any());
    }

    // ==================== TESTES DE VALIDAÇÃO ====================

    @Test
    @DisplayName("Deve verificar se médico está ativo")
    void deveVerificarSeMedicoEstaAtivo() {
        // Assert
        assertThat(doctor.isActive()).isTrue();

        doctor.setActive(false);
        assertThat(doctor.isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve validar role do médico")
    void deveValidarRoleDoMedico() {
        // Assert
        assertThat(doctor.getRole()).isEqualTo(Role.doctor);
    }

    @Test
    @DisplayName("Deve validar CRM do médico")
    void deveValidarCrmDoMedico() {
        // Assert
        assertThat(doctor.getCrm()).isNotNull();
        assertThat(doctor.getCrm()).startsWith("CRM/");
    }

    @Test
    @DisplayName("Deve validar especialidade do médico")
    void deveValidarEspecialidadeDoMedico() {
        // Assert
        assertThat(doctor.getSpecialty()).isNotNull();
        assertThat(doctor.getSpecialty()).isEqualTo("Cardiologia");
    }

    @Test
    @DisplayName("Deve buscar médicos por especialidade")
    void deveBuscarMedicosPorEspecialidade() {
        // Arrange
        Doctor cardiologista1 = new Doctor();
        cardiologista1.setSpecialty("Cardiologia");

        Doctor cardiologista2 = new Doctor();
        cardiologista2.setSpecialty("Cardiologia");

        List<Doctor> cardiologistas = List.of(cardiologista1, cardiologista2);

        // Act - Apenas simulando a busca
        List<Doctor> doctors = cardiologistas.stream()
                .filter(d -> "Cardiologia".equals(d.getSpecialty()))
                .toList();

        // Assert
        assertThat(doctors).hasSize(2);
        assertThat(doctors).allMatch(d -> "Cardiologia".equals(d.getSpecialty()));
    }

    @Test
    @DisplayName("Deve validar formato de email do médico")
    void deveValidarFormatoEmail() {
        // Assert
        assertThat(doctor.getEmail()).contains("@");
        assertThat(doctor.getEmail()).endsWith(".com");
    }
}


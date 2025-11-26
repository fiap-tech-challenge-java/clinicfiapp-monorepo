package br.com.fiap.clinic.scheduler.domain.service;

import br.com.fiap.clinic.scheduler.domain.entity.Patient;
import br.com.fiap.clinic.scheduler.domain.repository.PatientRepository;
import br.com.fiap.clinic.scheduler.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de gerenciamento de pacientes.
 * <p>
 * Responsável pelas operações de CRUD de pacientes no sistema.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;

    /**
     * Busca todos os pacientes cadastrados.
     *
     * @return lista de todos os pacientes
     */
    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    /**
     * Busca todos os pacientes com paginação.
     *
     * @param pageable configuração de paginação
     * @return página de pacientes
     */
    public Page<Patient> findAll(Pageable pageable) {
        return patientRepository.findAll(pageable);
    }

    /**
     * Busca um paciente por ID.
     *
     * @param id ID do paciente
     * @return paciente encontrado
     * @throws ResourceNotFoundException se o paciente não for encontrado
     */
    public Patient findById(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com ID: " + id));
    }

    /**
     * Cria um novo paciente.
     *
     * @param patient dados do paciente a ser criado
     * @return paciente criado
     */
    @Transactional
    public Patient create(Patient patient) {
        return patientRepository.save(patient);
    }

    /**
     * Atualiza os dados de um paciente existente.
     *
     * @param id ID do paciente a ser atualizado
     * @param patientDetails novos dados do paciente
     * @return paciente atualizado
     * @throws ResourceNotFoundException se o paciente não for encontrado
     */
    @Transactional
    public Patient update(UUID id, Patient patientDetails) {
        Patient patient = findById(id);

        patient.setName(patientDetails.getName());
        patient.setEmail(patientDetails.getEmail());
        patient.setLogin(patientDetails.getLogin());
        patient.setBirthDate(patientDetails.getBirthDate());
        patient.setIsActive(patientDetails.getIsActive());

        return patientRepository.save(patient);
    }

    /**
     * Desativa um paciente (soft delete).
     *
     * @param id ID do paciente a ser desativado
     * @throws ResourceNotFoundException se o paciente não for encontrado
     */
    @Transactional
    public void deactivate(UUID id) {
        Patient patient = findById(id);
        patient.setIsActive(false);
        patientRepository.save(patient);
    }

    /**
     * Ativa um paciente.
     *
     * @param id ID do paciente a ser ativado
     * @throws ResourceNotFoundException se o paciente não for encontrado
     */
    @Transactional
    public void activate(UUID id) {
        Patient patient = findById(id);
        patient.setIsActive(true);
        patientRepository.save(patient);
    }

    /**
     * Remove permanentemente um paciente.
     *
     * @param id ID do paciente a ser removido
     * @throws ResourceNotFoundException se o paciente não for encontrado
     */
    @Transactional
    public void delete(UUID id) {
        if (!patientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Paciente não encontrado com ID: " + id);
        }
        patientRepository.deleteById(id);
    }

    /**
     * Verifica se um paciente existe.
     *
     * @param id ID do paciente
     * @return true se o paciente existe, false caso contrário
     */
    public boolean exists(UUID id) {
        return patientRepository.existsById(id);
    }
}


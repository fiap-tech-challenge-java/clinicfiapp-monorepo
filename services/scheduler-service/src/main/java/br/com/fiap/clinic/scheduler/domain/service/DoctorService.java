package br.com.fiap.clinic.scheduler.domain.service;

import br.com.fiap.clinic.scheduler.domain.entity.Doctor;
import br.com.fiap.clinic.scheduler.domain.repository.DoctorRepository;
import br.com.fiap.clinic.scheduler.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de gerenciamento de médicos.
 * <p>
 * Responsável pelas operações de CRUD de médicos no sistema.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;

    /**
     * Busca todos os médicos cadastrados.
     *
     * @return lista de todos os médicos
     */
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    /**
     * Busca todos os médicos com paginação.
     *
     * @param pageable configuração de paginação
     * @return página de médicos
     */
    public Page<Doctor> findAll(Pageable pageable) {
        return doctorRepository.findAll(pageable);
    }

    /**
     * Busca um médico por ID.
     *
     * @param id ID do médico
     * @return médico encontrado
     * @throws ResourceNotFoundException se o médico não for encontrado
     */
    public Doctor findById(UUID id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado com ID: " + id));
    }

    /**
     * Cria um novo médico.
     *
     * @param doctor dados do médico a ser criado
     * @return médico criado
     */
    @Transactional
    public Doctor create(Doctor doctor) {
        return doctorRepository.save(doctor);
    }

    /**
     * Atualiza os dados de um médico existente.
     *
     * @param id ID do médico a ser atualizado
     * @param doctorDetails novos dados do médico
     * @return médico atualizado
     * @throws ResourceNotFoundException se o médico não for encontrado
     */
    @Transactional
    public Doctor update(UUID id, Doctor doctorDetails) {
        Doctor doctor = findById(id);

        doctor.setName(doctorDetails.getName());
        doctor.setEmail(doctorDetails.getEmail());
        doctor.setLogin(doctorDetails.getLogin());
        doctor.setCrm(doctorDetails.getCrm());
        doctor.setSpecialty(doctorDetails.getSpecialty());
        doctor.setIsActive(doctorDetails.getIsActive());

        return doctorRepository.save(doctor);
    }

    /**
     * Desativa um médico (soft delete).
     *
     * @param id ID do médico a ser desativado
     * @throws ResourceNotFoundException se o médico não for encontrado
     */
    @Transactional
    public void deactivate(UUID id) {
        Doctor doctor = findById(id);
        doctor.setIsActive(false);
        doctorRepository.save(doctor);
    }

    /**
     * Ativa um médico.
     *
     * @param id ID do médico a ser ativado
     * @throws ResourceNotFoundException se o médico não for encontrado
     */
    @Transactional
    public void activate(UUID id) {
        Doctor doctor = findById(id);
        doctor.setIsActive(true);
        doctorRepository.save(doctor);
    }

    /**
     * Remove permanentemente um médico.
     *
     * @param id ID do médico a ser removido
     * @throws ResourceNotFoundException se o médico não for encontrado
     */
    @Transactional
    public void delete(UUID id) {
        if (!doctorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Médico não encontrado com ID: " + id);
        }
        doctorRepository.deleteById(id);
    }

    /**
     * Verifica se um médico existe.
     *
     * @param id ID do médico
     * @return true se o médico existe, false caso contrário
     */
    public boolean exists(UUID id) {
        return doctorRepository.existsById(id);
    }
}


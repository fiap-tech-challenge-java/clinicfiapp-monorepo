package br.com.fiap.clinic.scheduler.domain.service;

import br.com.fiap.clinic.scheduler.domain.entity.Nurse;
import br.com.fiap.clinic.scheduler.domain.repository.NurseRepository;
import br.com.fiap.clinic.scheduler.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de gerenciamento de enfermeiros.
 * <p>
 * Responsável pelas operações de CRUD de enfermeiros no sistema.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NurseService {

    private final NurseRepository nurseRepository;

    /**
     * Busca todos os enfermeiros cadastrados.
     *
     * @return lista de todos os enfermeiros
     */
    public List<Nurse> findAll() {
        return nurseRepository.findAll();
    }

    /**
     * Busca todos os enfermeiros com paginação.
     *
     * @param pageable configuração de paginação
     * @return página de enfermeiros
     */
    public Page<Nurse> findAll(Pageable pageable) {
        return nurseRepository.findAll(pageable);
    }

    /**
     * Busca um enfermeiro por ID.
     *
     * @param userId ID do enfermeiro (user_id)
     * @return enfermeiro encontrado
     * @throws ResourceNotFoundException se o enfermeiro não for encontrado
     */
    public Nurse findById(UUID userId) {
        return nurseRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Enfermeiro não encontrado com ID: " + userId));
    }

    /**
     * Cria um novo enfermeiro.
     *
     * @param nurse dados do enfermeiro a ser criado
     * @return enfermeiro criado
     */
    @Transactional
    public Nurse create(Nurse nurse) {
        return nurseRepository.save(nurse);
    }

    /**
     * Atualiza os dados de um enfermeiro existente.
     *
     * @param userId ID do enfermeiro a ser atualizado
     * @param nurseDetails novos dados do enfermeiro
     * @return enfermeiro atualizado
     * @throws ResourceNotFoundException se o enfermeiro não for encontrado
     */
    @Transactional
    public Nurse update(UUID userId, Nurse nurseDetails) {
        Nurse nurse = findById(userId);

        if (nurseDetails.getName() != null) {
            nurse.setName(nurseDetails.getName());
        }
        if (nurseDetails.getEmail() != null) {
            nurse.setEmail(nurseDetails.getEmail());
        }
        if (nurseDetails.getLogin() != null) {
            nurse.setLogin(nurseDetails.getLogin());
        }
        // isActive agora é herdado de User e é boolean (não Boolean)
        nurse.setActive(nurseDetails.isActive());

        return nurseRepository.save(nurse);
    }

    /**
     * Desativa um enfermeiro (soft delete).
     *
     * @param userId ID do enfermeiro a ser desativado
     * @throws ResourceNotFoundException se o enfermeiro não for encontrado
     */
    @Transactional
    public void deactivate(UUID userId) {
        Nurse nurse = findById(userId);
        nurse.setActive(false);
        nurseRepository.save(nurse);
    }

    /**
     * Ativa um enfermeiro.
     *
     * @param userId ID do enfermeiro a ser ativado
     * @throws ResourceNotFoundException se o enfermeiro não for encontrado
     */
    @Transactional
    public void activate(UUID userId) {
        Nurse nurse = findById(userId);
        nurse.setActive(true);
        nurseRepository.save(nurse);
    }

    /**
     * Remove permanentemente um enfermeiro.
     *
     * @param userId ID do enfermeiro a ser removido
     * @throws ResourceNotFoundException se o enfermeiro não for encontrado
     */
    @Transactional
    public void delete(UUID userId) {
        if (!nurseRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Enfermeiro não encontrado com ID: " + userId);
        }
        nurseRepository.deleteById(userId);
    }

    /**
     * Verifica se um enfermeiro existe.
     *
     * @param userId ID do enfermeiro
     * @return true se o enfermeiro existe, false caso contrário
     */
    public boolean exists(UUID userId) {
        return nurseRepository.existsById(userId);
    }
}


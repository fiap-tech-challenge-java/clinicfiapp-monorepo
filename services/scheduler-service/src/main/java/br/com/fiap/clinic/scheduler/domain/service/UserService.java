package br.com.fiap.clinic.scheduler.domain.service;

import br.com.fiap.clinic.scheduler.domain.entity.User;
import br.com.fiap.clinic.scheduler.domain.repository.UserRepository;
import br.com.fiap.clinic.scheduler.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de gerenciamento de usuários.
 * <p>
 * Responsável pelas operações de CRUD de usuários no sistema.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * Busca todos os usuários cadastrados.
     *
     * @return lista de todos os usuários
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Busca todos os usuários com paginação.
     *
     * @param pageable configuração de paginação
     * @return página de usuários
     */
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Busca um usuário por ID.
     *
     * @param id ID do usuário
     * @return usuário encontrado
     * @throws ResourceNotFoundException se o usuário não for encontrado
     */
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + id));
    }

    /**
     * Busca um usuário por login.
     *
     * @param login login do usuário
     * @return usuário encontrado
     * @throws ResourceNotFoundException se o usuário não for encontrado
     */
    public User findByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com login: " + login));
    }

    /**
     * Cria um novo usuário.
     *
     * @param user dados do usuário a ser criado
     * @return usuário criado
     */
    @Transactional
    public User create(User user) {
        return userRepository.save(user);
    }

    /**
     * Atualiza os dados de um usuário existente.
     *
     * @param id ID do usuário a ser atualizado
     * @param userDetails novos dados do usuário
     * @return usuário atualizado
     * @throws ResourceNotFoundException se o usuário não for encontrado
     */
    @Transactional
    public User update(UUID id, User userDetails) {
        User user = findById(id);

        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        user.setLogin(userDetails.getLogin());
        user.setActive(userDetails.isActive());

        return userRepository.save(user);
    }

    /**
     * Desativa um usuário (soft delete).
     *
     * @param id ID do usuário a ser desativado
     * @throws ResourceNotFoundException se o usuário não for encontrado
     */
    @Transactional
    public void deactivate(UUID id) {
        User user = findById(id);
        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Ativa um usuário.
     *
     * @param id ID do usuário a ser ativado
     * @throws ResourceNotFoundException se o usuário não for encontrado
     */
    @Transactional
    public void activate(UUID id) {
        User user = findById(id);
        user.setActive(true);
        userRepository.save(user);
    }

    /**
     * Remove permanentemente um usuário do sistema (hard delete).
     * <p>
     * Atenção: Esta operação é irreversível.
     *
     * @param id ID do usuário a ser removido
     * @throws ResourceNotFoundException se o usuário não for encontrado
     */
    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Usuário não encontrado com ID: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Verifica se um usuário existe no sistema.
     *
     * @param id ID do usuário
     * @return true se o usuário existe, false caso contrário
     */
    public boolean exists(UUID id) {
        return userRepository.existsById(id);
    }
}


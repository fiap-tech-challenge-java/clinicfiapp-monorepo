package br.com.fiap.clinic.history.domain.service;

import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.repository.ProjectedAppointmentHistoryRepository;
import br.com.fiap.clinic.history.exception.HistoryAccessDeniedException;
import br.com.fiap.clinic.history.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serviço de controle de acesso ao histórico de consultas.
 * <p>
 * Hierarquia de acesso por role:
 * <ul>
 *   <li><b>ROLE_MEDICO</b>: Visualizar e editar históricos</li>
 *   <li><b>ROLE_ENFERMEIRO</b>: Visualizar históricos (read-only)</li>
 *   <li><b>ROLE_PACIENTE</b>: Visualizar apenas o próprio histórico</li>
 * </ul>
 * Usuários com múltiplas roles têm o acesso mais amplo.
 * <p>
 * <b>Nota</b>: O registro de novas consultas é feito automaticamente pelo serviço de agendamento.
 */
@Service
@RequiredArgsConstructor
public class HistoryProjectionService {

    private final ProjectedAppointmentHistoryRepository historyRepository;

    /**
     * Recupera o histórico de consultas de um paciente.
     * <p>
     * Controle de acesso:
     * <ul>
     *   <li>Médicos: visualização de qualquer histórico</li>
     *   <li>Enfermeiros: visualização de qualquer histórico</li>
     *   <li>Pacientes: visualização apenas do próprio histórico</li>
     * </ul>
     *
     * @param patientId ID do paciente (não nulo)
     * @return histórico de consultas do paciente
     * @throws IllegalArgumentException     se patientId for nulo
     * @throws HistoryAccessDeniedException se acesso negado ou usuário não autenticado
     */
    public List<ProjectedAppointmentHistory> getHistoryForPatient(Long patientId) {
        if (patientId == null) {
            throw new IllegalArgumentException("O ID do paciente não pode ser nulo.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = getRoles(authentication);

        if (roles.contains("ROLE_MEDICO") || roles.contains("ROLE_ENFERMEIRO")) {
            return historyRepository.findByPatientId(patientId);
        }

        if (roles.contains("ROLE_PACIENTE")) {
            Long authenticatedPatientId = getUserIdFromAuthentication(authentication);

            if (!Objects.equals(patientId, authenticatedPatientId)) {
                throw new HistoryAccessDeniedException("Paciente só pode visualizar o próprio histórico.");
            }

            return historyRepository.findByPatientId(patientId);
        }

        throw new HistoryAccessDeniedException("Acesso negado ao histórico de consultas.");
    }

    /**
     * Atualiza um registro de histórico de consulta.
     * <p>
     * Controle de acesso:
     * <ul>
     *   <li>Apenas <b>ROLE_MEDICO</b> pode editar históricos</li>
     *   <li>Enfermeiros e pacientes têm acesso read-only</li>
     * </ul>
     *
     * @param history entidade de histórico a ser atualizada (não nula, ID obrigatório)
     * @return histórico atualizado
     * @throws IllegalArgumentException     se history for nulo ou não possuir ID
     * @throws HistoryAccessDeniedException se usuário não for médico ou não autenticado
     * @throws ResourceNotFoundException    se histórico com o ID fornecido não existir
     */
    public ProjectedAppointmentHistory updateHistory(ProjectedAppointmentHistory history) {
        if (history == null) {
            throw new IllegalArgumentException("O histórico não pode ser nulo.");
        }
        if (history.getId() == null) {
            throw new IllegalArgumentException("Para atualizar um histórico, o ID é obrigatório.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = getRoles(authentication);

        if (!roles.contains("ROLE_MEDICO")) {
            throw new HistoryAccessDeniedException("Apenas médicos podem editar históricos de consultas.");
        }

        ProjectedAppointmentHistory existingHistory = historyRepository.findById(history.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Histórico com ID " + history.getId() + " não encontrado."
                ));


        existingHistory.setStatus(history.getStatus());
        existingHistory.setLastAction(history.getLastAction());

        return historyRepository.save(existingHistory);
    }

    /**
     * Extrai as roles do usuário autenticado.
     *
     * @param authentication objeto de autenticação
     * @return conjunto de roles
     * @throws HistoryAccessDeniedException se não autenticado ou usuário anônimo
     */
    private Set<String> getRoles(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new HistoryAccessDeniedException("Usuário não autenticado.");
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    /**
     * Extrai o ID do usuário do principal (authentication.getName()).
     * <p>
     * Pressupõe que o sistema armazena o ID numérico como principal.
     * Exemplo: usuário "fernando" com ID 55 → getName() retorna "55".
     *
     * @param authentication objeto de autenticação
     * @return ID do usuário
     * @throws HistoryAccessDeniedException se ID não for numérico
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new HistoryAccessDeniedException("ID de usuário inválido.");
        }
    }
}
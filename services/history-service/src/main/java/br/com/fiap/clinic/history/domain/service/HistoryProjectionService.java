package br.com.fiap.clinic.history.domain.service;

import br.com.fiap.clinic.history.config.security.CustomUserDetails;
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
import org.springframework.transaction.annotation.Transactional;

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
 */
@Service
@RequiredArgsConstructor
public class HistoryProjectionService {

    private static final String ROLE_MEDICO = "ROLE_MEDICO";
    private static final String ROLE_ENFERMEIRO = "ROLE_ENFERMEIRO";
    private static final String ROLE_PACIENTE = "ROLE_PACIENTE";

    private final ProjectedAppointmentHistoryRepository historyRepository;

    @Transactional
    public ProjectedAppointmentHistory createHistory(ProjectedAppointmentHistory history) {
        if (history == null) {
            throw new IllegalArgumentException("O histórico não pode ser nulo.");
        }
        if (history.getId() != null) {
            throw new IllegalArgumentException("Para criar um histórico, o ID deve ser nulo.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = getRoles(authentication);

        if (!roles.contains(ROLE_MEDICO)) {
            throw new HistoryAccessDeniedException("Apenas médicos podem criar históricos de consultas.");
        }

        return historyRepository.save(history);
    }

    public ProjectedAppointmentHistory getHistoryById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID não pode ser nulo.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = getRoles(authentication);

        ProjectedAppointmentHistory history = historyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Histórico com ID " + id + " não encontrado."));

        if (roles.contains(ROLE_MEDICO) || roles.contains(ROLE_ENFERMEIRO)) {
            return history;
        }

        if (roles.contains(ROLE_PACIENTE)) {
            Long authenticatedPatientId = getUserIdFromAuthentication(authentication);
            if (!Objects.equals(history.getPatientId(), authenticatedPatientId)) {
                throw new HistoryAccessDeniedException("Paciente só pode visualizar o próprio histórico.");
            }
            return history;
        }

        throw new HistoryAccessDeniedException("Acesso negado ao histórico.");
    }

    public List<ProjectedAppointmentHistory> getAllHistories() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = getRoles(authentication);

        if (roles.contains(ROLE_MEDICO) || roles.contains(ROLE_ENFERMEIRO)) {
            return historyRepository.findAll();
        }

        if (roles.contains(ROLE_PACIENTE)) {
            Long authenticatedPatientId = getUserIdFromAuthentication(authentication);
            return historyRepository.findByPatientId(authenticatedPatientId);
        }

        throw new HistoryAccessDeniedException("Acesso negado aos históricos.");
    }

    public List<ProjectedAppointmentHistory> getHistoryForPatient(Long patientId) {
        if (patientId == null) {
            throw new IllegalArgumentException("O ID do paciente não pode ser nulo.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = getRoles(authentication);

        if (roles.contains(ROLE_MEDICO) || roles.contains(ROLE_ENFERMEIRO)) {
            return historyRepository.findByPatientId(patientId);
        }

        if (roles.contains(ROLE_PACIENTE)) {
            Long authenticatedPatientId = getUserIdFromAuthentication(authentication);

            if (!Objects.equals(patientId, authenticatedPatientId)) {
                throw new HistoryAccessDeniedException("Paciente só pode visualizar o próprio histórico.");
            }

            return historyRepository.findByPatientId(patientId);
        }

        throw new HistoryAccessDeniedException("Acesso negado ao histórico de consultas.");
    }

    @Transactional
    public ProjectedAppointmentHistory updateHistory(ProjectedAppointmentHistory history) {
        if (history == null) {
            throw new IllegalArgumentException("O histórico não pode ser nulo.");
        }
        if (history.getId() == null) {
            throw new IllegalArgumentException("Para atualizar um histórico, o ID é obrigatório.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = getRoles(authentication);

        if (!roles.contains(ROLE_MEDICO)) {
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

    @Transactional
    public void deleteHistory(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID não pode ser nulo.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = getRoles(authentication);

        if (!roles.contains(ROLE_MEDICO)) {
            throw new HistoryAccessDeniedException("Apenas médicos podem deletar históricos de consultas.");
        }

        if (!historyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Histórico com ID " + id + " não encontrado.");
        }

        historyRepository.deleteById(id);
    }

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

    private Long getUserIdFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;
            return userDetails.userId();
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new HistoryAccessDeniedException(
                    "Não foi possível extrair ID do usuário. " +
                            "O principal deve ser CustomUserDetails ou getName() deve retornar um ID numérico."
            );
        }
    }
}
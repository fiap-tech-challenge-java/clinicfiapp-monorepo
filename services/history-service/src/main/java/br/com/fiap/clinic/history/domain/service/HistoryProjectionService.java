package br.com.fiap.clinic.history.domain.service;

import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.repository.ProjectedAppointmentHistoryRepository;
import br.com.fiap.clinic.history.exception.HistoryAccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryProjectionService {

    private final ProjectedAppointmentHistoryRepository historyRepository;

    public List<ProjectedAppointmentHistory> getHistoryForPatient(Long patientId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = getRoles(authentication);

        if (roles.contains("ROLE_PACIENTE")) {
            Long authenticatedPatientId = getUserIdFromAuthentication(authentication);

            if (!patientId.equals(authenticatedPatientId)) {
                throw new HistoryAccessDeniedException("Paciente só pode visualizar o próprio histórico.");
            }

            return historyRepository.findByPatientId(patientId);
        }

        if (roles.contains("ROLE_MEDICO") || roles.contains("ROLE_ENFERMEIRO")) {
            return historyRepository.findByPatientId(patientId);
        }

        throw new HistoryAccessDeniedException("Acesso negado ao histórico de consultas.");

    }

    private Set<String> getRoles(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new HistoryAccessDeniedException("Usuário não autenticado.");
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new HistoryAccessDeniedException("ID de usuário inválido.");
        }
    }
}
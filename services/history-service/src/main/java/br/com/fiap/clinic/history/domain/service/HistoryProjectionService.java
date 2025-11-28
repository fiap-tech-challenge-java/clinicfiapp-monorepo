package br.com.fiap.clinic.history.domain.service;

import br.com.fiap.clinic.history.config.security.CustomUserDetails;
import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.repository.ProjectedAppointmentHistoryRepository;
import br.com.fiap.clinic.history.exception.HistoryAccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryProjectionService {

    private static final String ROLE_DOCTOR = "ROLE_doctor";
    private static final String ROLE_NURSE = "ROLE_nurse";
    private static final String ROLE_PATIENT = "ROLE_patient";

    private final ProjectedAppointmentHistoryRepository historyRepository;

    public List<ProjectedAppointmentHistory> getHistory(String patientId, String patientName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = getRoles(authentication);
        String currentUserId = getUserIdStringFromAuthentication(authentication);

        if (roles.contains(ROLE_PATIENT)) {
            try {
                UUID patientUuid = UUID.fromString(currentUserId);
                return historyRepository.findByPatientId(patientUuid);
            } catch (IllegalArgumentException e) {
                throw new HistoryAccessDeniedException("ID do paciente inválido.");
            }
        }

        if (roles.contains(ROLE_DOCTOR) || roles.contains(ROLE_NURSE)) {
            if (patientId != null && !patientId.isBlank()) {
                try {
                    UUID patientUuid = UUID.fromString(patientId);
                    return historyRepository.findByPatientId(patientUuid);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("ID do paciente inválido: " + patientId);
                }
            }
            if (patientName != null && !patientName.isBlank()) {
                return historyRepository.findByPatientNameContainingIgnoreCase(patientName);
            }
            return historyRepository.findAll();
        }

        throw new HistoryAccessDeniedException("Acesso negado ao histórico de consultas.");
    }

    private Set<String> getRoles(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new HistoryAccessDeniedException("Usuário não autenticado.");
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private String getUserIdStringFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return String.valueOf(userDetails.userId());
        }

        return authentication.getName();
    }

    @Transactional
    public void createHistoryFromKafka(ProjectedAppointmentHistory history) {
        if (history == null) {
            throw new IllegalArgumentException("O histórico vindo do Kafka não pode ser nulo.");
        }
        historyRepository.save(history);
    }
}
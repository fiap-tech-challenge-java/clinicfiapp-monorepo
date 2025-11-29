package br.com.fiap.clinic.history.domain.service;

import br.com.fiap.clinic.history.config.security.CustomUserDetails;
import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.repository.ProjectedAppointmentHistoryRepository;
import br.com.fiap.clinic.history.exception.HistoryAccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryProjectionService {

    private static final String ROLE_DOCTOR = "ROLE_doctor";
    private static final String ROLE_NURSE = "ROLE_nurse";
    private static final String ROLE_PATIENT = "ROLE_patient";

    private final ProjectedAppointmentHistoryRepository historyRepository;

    public List<ProjectedAppointmentHistory> getHistory(
            String patientIdStr,
            String patientName,
            String doctorIdStr,
            String dateStr,
            String status
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = getRoles(authentication);
        String currentUserIdStr = getUserIdStringFromAuthentication(authentication);
        UUID currentUserId = UUID.fromString(currentUserIdStr);

        // --- REGRA 1: PACIENTE ---
        // Paciente só vê suas próprias consultas, ignorando outros filtros de ID
        if (roles.contains(ROLE_PATIENT)) {
            return historyRepository.findByPatientId(currentUserId);
        }

        // --- REGRA 2: MÉDICO E ENFERMEIRO ---
        if (roles.contains(ROLE_DOCTOR) || roles.contains(ROLE_NURSE)) {

            UUID targetPatientId = parseUUID(patientIdStr);
            UUID targetDoctorId = parseUUID(doctorIdStr);
            LocalDateTime startAt = null;
            LocalDateTime endAt = null;

            // Se for MÉDICO e não especificou outro médico, assume que quer ver as DELE
            if (roles.contains(ROLE_DOCTOR) && targetDoctorId == null) {
                targetDoctorId = currentUserId;
            }

            // Tratamento da Data (String "YYYY-MM-DD" -> Intervalo do dia)
            if (dateStr != null && !dateStr.isBlank()) {
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    startAt = date.atStartOfDay();
                    endAt = date.atTime(LocalTime.MAX);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Formato de data inválido. Use YYYY-MM-DD");
                }
            }

            // Tratamento busca por nome (Se tiver nome, a busca customizada pode ser complexa,
            // então mantemos a busca simples por nome OU a busca customizada pelos outros campos)
            if (patientName != null && !patientName.isBlank()) {
                return historyRepository.findByPatientNameContainingIgnoreCase(patientName);
            }

            // Busca usando a Query Dinâmica no Repositório
            return historyRepository.findCustom(
                    targetPatientId,
                    targetDoctorId,
                    status,
                    startAt,
                    endAt
            );
        }

        throw new HistoryAccessDeniedException("Acesso negado ao histórico de consultas.");
    }

    private UUID parseUUID(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ID inválido: " + uuidStr);
        }
    }

    // ... (Métodos getRoles, getUserIdStringFromAuthentication e createHistoryFromKafka mantêm-se iguais)

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
        if (history == null) throw new IllegalArgumentException("Histórico nulo.");
        historyRepository.save(history);
    }
}
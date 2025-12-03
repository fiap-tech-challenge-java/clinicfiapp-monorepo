package br.com.fiap.clinic.history.domain.service;

import br.com.fiap.clinic.history.config.security.CustomUserDetails;
import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.repository.ProjectedAppointmentHistoryRepository;
import br.com.fiap.clinic.history.exception.HistoryAccessDeniedException;
import jakarta.persistence.criteria.Predicate; // <--- Importante
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification; // <--- Importante
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
import java.util.ArrayList;
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
        // Paciente só vê suas próprias consultas
        if (roles.contains(ROLE_PATIENT)) {
            // Usa Specification aqui também para manter padrão, ou o método findByPatientId
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

            // Tratamento da Data
            if (dateStr != null && !dateStr.isBlank()) {
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    startAt = date.atStartOfDay();
                    endAt = date.atTime(LocalTime.MAX);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Formato de data inválido. Use YYYY-MM-DD");
                }
            }

            final UUID finalPatientId = targetPatientId;
            final UUID finalDoctorId = targetDoctorId;
            final LocalDateTime finalStartAt = startAt;
            final LocalDateTime finalEndAt = endAt;
            final String finalPatientName = patientName;

            Specification<ProjectedAppointmentHistory> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (finalPatientId != null) {
                    predicates.add(cb.equal(root.get("patientId"), finalPatientId));
                }

                if (finalPatientName != null && !finalPatientName.isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("patientName")), "%" + finalPatientName.toLowerCase() + "%"));
                }

                if (finalDoctorId != null) {
                    predicates.add(cb.equal(root.get("doctorId"), finalDoctorId));
                }

                if (status != null && !status.isBlank()) {
                    predicates.add(cb.equal(root.get("status"), status));
                }

                if (finalStartAt != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("startAt"), finalStartAt));
                }

                if (finalEndAt != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("startAt"), finalEndAt));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            return historyRepository.findAll(spec);
        }

        throw new HistoryAccessDeniedException("Acesso negado ao histórico de consultas.");
    }

    // ... métodos auxiliares (parseUUID, getRoles, etc.) mantêm-se iguais ...
    private UUID parseUUID(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ID inválido: " + uuidStr);
        }
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
        if (history == null) throw new IllegalArgumentException("Histórico nulo.");
        historyRepository.save(history);
    }
}
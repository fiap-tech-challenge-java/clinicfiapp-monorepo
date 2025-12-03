package br.com.fiap.clinic.scheduler.controller.graphql;

import br.com.fiap.clinic.scheduler.domain.entity.Patient;
import br.com.fiap.clinic.scheduler.domain.entity.Role;
import br.com.fiap.clinic.scheduler.domain.service.PatientService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PatientGraphQLController {

    private final PatientService patientService;
    private final PasswordEncoder passwordEncoder;

    // --- Records para Inputs (DTOs) ---

    record CreatePatientInput(
            @NotBlank(message = "Nome é obrigatório") String name,
            @NotBlank(message = "Email é obrigatório") String email,
            @NotBlank(message = "Login é obrigatório") String login,
            @NotBlank(message = "Senha é obrigatória") String password,
            String birthDate
    ) {}

    record UpdatePatientInput(
            String name,
            String email,
            String login,
            String birthDate,
            Boolean isActive
    ) {}

    // --- QUERIES ---

    @QueryMapping
    @PreAuthorize("hasAnyRole('nurse', 'doctor')")
    public List<Patient> patients() {
        log.info("Buscando todos os pacientes");
        return patientService.findAll();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('nurse', 'doctor', 'patient')")
    public Patient patient(@Argument String id) {
        log.info("Buscando paciente por ID: {}", id);
        return patientService.findById(UUID.fromString(id));
    }

    // --- MUTATIONS ---

    @MutationMapping
    @PreAuthorize("hasAnyRole('nurse', 'doctor')")
    public Patient createPatient(@Argument CreatePatientInput input) {
        log.info("Criando novo paciente: {}", input.login);

        Patient patient = new Patient();
        patient.setName(input.name);
        patient.setEmail(input.email);
        patient.setLogin(input.login);
        patient.setPassword(passwordEncoder.encode(input.password));
        patient.setRole(Role.patient);

        if (input.birthDate != null && !input.birthDate.isBlank()) {
            patient.setBirthDate(LocalDate.parse(input.birthDate));
        }

        return patientService.create(patient);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('nurse', 'doctor')")
    public Patient updatePatient(@Argument String id, @Argument UpdatePatientInput input) {
        log.info("Atualizando paciente ID: {}", id);

        Patient patient = patientService.findById(UUID.fromString(id));

        if (input.name != null) {
            patient.setName(input.name);
        }
        if (input.email != null) {
            patient.setEmail(input.email);
        }
        if (input.login != null) {
            patient.setLogin(input.login);
        }
        if (input.birthDate != null && !input.birthDate.isBlank()) {
            patient.setBirthDate(LocalDate.parse(input.birthDate));
        }
        if (input.isActive != null) {
            patient.setActive(input.isActive);
        }

        return patientService.update(UUID.fromString(id), patient);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('nurse', 'doctor')")
    public Boolean deactivatePatient(@Argument String id) {
        log.info("Desativando paciente ID: {}", id);
        patientService.deactivate(UUID.fromString(id));
        return true;
    }
}


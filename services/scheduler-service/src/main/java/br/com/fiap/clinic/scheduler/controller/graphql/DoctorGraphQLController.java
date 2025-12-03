package br.com.fiap.clinic.scheduler.controller.graphql;

import br.com.fiap.clinic.scheduler.domain.entity.Doctor;
import br.com.fiap.clinic.scheduler.domain.entity.Role;
import br.com.fiap.clinic.scheduler.domain.service.DoctorService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DoctorGraphQLController {

    private final DoctorService doctorService;
    private final PasswordEncoder passwordEncoder;

    // --- Records para Inputs (DTOs) ---

    record CreateDoctorInput(
            @NotBlank(message = "Nome é obrigatório") String name,
            @NotBlank(message = "Email é obrigatório") String email,
            @NotBlank(message = "Login é obrigatório") String login,
            @NotBlank(message = "Senha é obrigatória") String password,
            @NotBlank(message = "CRM é obrigatório") String crm,
            @NotBlank(message = "Especialidade é obrigatória") String specialty
    ) {}

    record UpdateDoctorInput(
            String name,
            String email,
            String login,
            String crm,
            String specialty,
            Boolean isActive
    ) {}

    // --- QUERIES ---

    @QueryMapping
    @PreAuthorize("hasAnyRole('doctor', 'nurse')")
    public List<Doctor> doctors() {
        log.info("Buscando todos os médicos");
        return doctorService.findAll();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('doctor', 'nurse')")
    public Doctor doctor(@Argument String id) {
        log.info("Buscando médico por ID: {}", id);
        return doctorService.findById(UUID.fromString(id));
    }

    // --- MUTATIONS ---

    @MutationMapping
    @PreAuthorize("hasRole('nurse')")
    public Doctor createDoctor(@Argument CreateDoctorInput input) {
        log.info("Criando novo médico: {}", input.login);

        Doctor doctor = new Doctor();
        doctor.setName(input.name);
        doctor.setEmail(input.email);
        doctor.setLogin(input.login);
        doctor.setPassword(passwordEncoder.encode(input.password));
        doctor.setRole(Role.doctor);
        doctor.setCrm(input.crm);
        doctor.setSpecialty(input.specialty);

        return doctorService.create(doctor);
    }

    @MutationMapping
    @PreAuthorize("hasRole('nurse')")
    public Doctor updateDoctor(@Argument String id, @Argument UpdateDoctorInput input) {
        log.info("Atualizando médico ID: {}", id);

        Doctor doctor = doctorService.findById(UUID.fromString(id));

        if (input.name != null) {
            doctor.setName(input.name);
        }
        if (input.email != null) {
            doctor.setEmail(input.email);
        }
        if (input.login != null) {
            doctor.setLogin(input.login);
        }
        if (input.crm != null) {
            doctor.setCrm(input.crm);
        }
        if (input.specialty != null) {
            doctor.setSpecialty(input.specialty);
        }
        if (input.isActive != null) {
            doctor.setActive(input.isActive);
        }

        return doctorService.update(UUID.fromString(id), doctor);
    }

    @MutationMapping
    @PreAuthorize("hasRole('nurse')")
    public Boolean deactivateDoctor(@Argument String id) {
        log.info("Desativando médico ID: {}", id);
        doctorService.deactivate(UUID.fromString(id));
        return true;
    }
}


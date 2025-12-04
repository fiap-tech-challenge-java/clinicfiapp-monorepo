package br.com.fiap.clinic.scheduler.controller.graphql;

import br.com.fiap.clinic.scheduler.domain.entity.Nurse;
import br.com.fiap.clinic.scheduler.domain.entity.Role;
import br.com.fiap.clinic.scheduler.domain.service.NurseService;
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
public class NurseGraphQLController {

    private final NurseService nurseService;
    private final PasswordEncoder passwordEncoder;

    // --- Records para Inputs (DTOs) ---

    record CreateNurseInput(
            @NotBlank(message = "Nome é obrigatório") String name,
            @NotBlank(message = "Email é obrigatório") String email,
            @NotBlank(message = "Login é obrigatório") String login,
            @NotBlank(message = "Senha é obrigatória") String password
    ) {}

    record UpdateNurseInput(
            String name,
            String email,
            String login,
            Boolean isActive
    ) {}

    // --- QUERIES ---

    @QueryMapping
    @PreAuthorize("hasRole('nurse')")
    public List<Nurse> nurses() {
        log.info("Buscando todos os enfermeiros");
        return nurseService.findAll();
    }

    @QueryMapping
    @PreAuthorize("hasRole('nurse')")
    public Nurse nurse(@Argument String id) {
        log.info("Buscando enfermeiro por ID: {}", id);
        return nurseService.findById(UUID.fromString(id));
    }

    // --- MUTATIONS ---

    @MutationMapping
    @PreAuthorize("hasRole('nurse')")
    public Nurse createNurse(@Argument CreateNurseInput input) {
        log.info("Criando novo enfermeiro: {}", input.login);

        Nurse nurse = new Nurse();
        nurse.setName(input.name);
        nurse.setEmail(input.email);
        nurse.setLogin(input.login);
        nurse.setPassword(passwordEncoder.encode(input.password));
        nurse.setRole(Role.nurse);

        return nurseService.create(nurse);
    }

    @MutationMapping
    @PreAuthorize("hasRole('nurse')")
    public Nurse updateNurse(@Argument String id, @Argument UpdateNurseInput input) {
        log.info("Atualizando enfermeiro ID: {}", id);

        UUID nurseId = UUID.fromString(id);
        Nurse nurse = nurseService.findById(nurseId);

        if (input.name != null) {
            nurse.setName(input.name);
        }
        if (input.email != null) {
            nurse.setEmail(input.email);
        }
        if (input.login != null) {
            nurse.setLogin(input.login);
        }
        if (input.isActive != null) {
            nurse.setActive(input.isActive);
        }

        return nurseService.update(nurseId, nurse);
    }

    @MutationMapping
    @PreAuthorize("hasRole('nurse')")
    public Boolean deactivateNurse(@Argument String id) {
        log.info("Desativando enfermeiro ID: {}", id);
        nurseService.deactivate(UUID.fromString(id));
        return true;
    }
}


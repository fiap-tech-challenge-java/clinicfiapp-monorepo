package br.com.fiap.clinic.scheduler.controller.graphql;

import br.com.fiap.clinic.scheduler.domain.entity.Appointment;
import br.com.fiap.clinic.scheduler.domain.entity.AppointmentHistory;
import br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus;
import br.com.fiap.clinic.scheduler.domain.entity.User;
import br.com.fiap.clinic.scheduler.domain.repository.UserRepository;
import br.com.fiap.clinic.scheduler.domain.service.AppointmentService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AppointmentGraphQLController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    // --- Records para Inputs (DTOs) ---

    record CreateAppointmentInput(
            @NotBlank(message = "ID do paciente é obrigatório") String patientId,
            @NotBlank(message = "ID do médico é obrigatório") String doctorId,
            @NotNull(message = "Data de início é obrigatória") String startAt,
            @NotNull(message = "Data de término é obrigatória") String endAt
    ) {}

    record RescheduleAppointmentInput(
            @NotBlank String appointmentId,
            @NotNull String newStartAt,
            @NotNull String newEndAt
    ) {}


    // --- QUERIES ---

    @QueryMapping
    @Secured({"ROLE_doctor", "ROLE_nurse", "ROLE_patient"})
    public List<Appointment> appointments() {
        User user = getCurrentUser();
        return appointmentService.findAll(user);
    }

    @QueryMapping
    @Secured({"ROLE_doctor", "ROLE_nurse", "ROLE_patient"})
    public Appointment appointment(@Argument String id) {
        return appointmentService.findById(UUID.fromString(id));
    }

    @QueryMapping
    @Secured({"ROLE_doctor", "ROLE_nurse"})
    public List<Appointment> appointmentsByStatus(@Argument AppointmentStatus status) {
        return appointmentService.findByStatus(status);
    }

    @QueryMapping
    @Secured({"ROLE_doctor", "ROLE_nurse"})
    public List<AppointmentHistory> appointmentHistory(@Argument String appointmentId) {
        return appointmentService.findAppointmentHistory(UUID.fromString(appointmentId));
    }

    // --- MUTATIONS ---

    @MutationMapping
    @Secured({"ROLE_nurse", "ROLE_doctor"}) // Apenas equipe médica cria
    public Appointment createAppointment(@Argument CreateAppointmentInput input) {
        User currentUser = getCurrentUser();

        return appointmentService.createAppointment(
                UUID.fromString(input.patientId),
                UUID.fromString(input.doctorId),
                currentUser.getId(),
                OffsetDateTime.parse(input.startAt, DateTimeFormatter.ISO_DATE_TIME),
                OffsetDateTime.parse(input.endAt, DateTimeFormatter.ISO_DATE_TIME)
        );
    }

    @MutationMapping
    @Secured({"ROLE_nurse", "ROLE_doctor"})
    public Appointment confirmAppointment(@Argument String id) {
        return appointmentService.confirmAppointment(UUID.fromString(id));
    }

    @MutationMapping
    @Secured({"ROLE_nurse", "ROLE_doctor"})
    public Appointment cancelAppointment(@Argument String id) {
        return appointmentService.cancelAppointment(UUID.fromString(id));
    }

    @MutationMapping
    @Secured({"ROLE_doctor"}) // Apenas médico finaliza consulta
    public Appointment completeAppointment(@Argument String id) {
        return appointmentService.completeAppointment(UUID.fromString(id));
    }

    @MutationMapping
    @Secured({"ROLE_nurse", "ROLE_doctor"})
    public Appointment rescheduleAppointment(@Argument RescheduleAppointmentInput input) {
        return appointmentService.rescheduleAppointment(
                UUID.fromString(input.appointmentId),
                OffsetDateTime.parse(input.newStartAt, DateTimeFormatter.ISO_DATE_TIME),
                OffsetDateTime.parse(input.newEndAt, DateTimeFormatter.ISO_DATE_TIME)
        );
    }

    // --- Helper ---

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado: " + username));
    }
}
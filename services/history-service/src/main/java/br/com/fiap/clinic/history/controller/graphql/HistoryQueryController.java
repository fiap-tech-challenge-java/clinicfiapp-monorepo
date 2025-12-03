package br.com.fiap.clinic.history.controller.graphql;

import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.service.HistoryProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HistoryQueryController {

    private static final String ROLE_DOCTOR = "ROLE_doctor";
    private static final String ROLE_NURSE = "ROLE_nurse";
    private static final String ROLE_PATIENT = "ROLE_patient";

    private final HistoryProjectionService historyService;

    @QueryMapping
    @Secured({ROLE_PATIENT, ROLE_DOCTOR, ROLE_NURSE})
    public List<ProjectedAppointmentHistory> history(
            @Argument String patientId,
            @Argument String patientName,
            @Argument String doctorId,
            @Argument String date,
            @Argument String status
    ) {
        return historyService.getHistory(patientId, patientName, doctorId, date, status);
    }
}
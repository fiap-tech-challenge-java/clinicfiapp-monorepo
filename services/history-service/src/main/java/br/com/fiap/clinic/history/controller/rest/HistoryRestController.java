package br.com.fiap.clinic.history.controller.rest;

import br.com.fiap.clinic.history.domain.entity.ProjectedAppointmentHistory;
import br.com.fiap.clinic.history.domain.service.HistoryProjectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Tag(name = "Histórico de Consultas", description = "CRUD completo do Histórico")
public class HistoryRestController {

    private final HistoryProjectionService historyService;

    @PostMapping
    @Operation(summary = "Criar novo registro de histórico", description = "Apenas médicos podem criar registros.")
    public ResponseEntity<ProjectedAppointmentHistory> create(@RequestBody ProjectedAppointmentHistory history) {
        ProjectedAppointmentHistory created = historyService.createHistory(history);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar todos os históricos", description = "Médicos/Enfermeiros: todos | Pacientes: apenas próprio histórico")
    public ResponseEntity<List<ProjectedAppointmentHistory>> findAll() {
        List<ProjectedAppointmentHistory> histories = historyService.getAllHistories();
        return ResponseEntity.ok(histories);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Listar histórico por Paciente")
    public ResponseEntity<List<ProjectedAppointmentHistory>> findByPatient(@PathVariable Long patientId) {
        List<ProjectedAppointmentHistory> list = historyService.getHistoryForPatient(patientId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar registro único por ID")
    public ResponseEntity<ProjectedAppointmentHistory> findById(@PathVariable Long id) {
        ProjectedAppointmentHistory history = historyService.getHistoryById(id);
        return ResponseEntity.ok(history);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar registro", description = "Apenas médicos podem editar.")
    public ResponseEntity<ProjectedAppointmentHistory> update(@PathVariable Long id, @RequestBody ProjectedAppointmentHistory historyDetails) {
        historyDetails.setId(id);
        ProjectedAppointmentHistory updated = historyService.updateHistory(historyDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir registro", description = "Apenas médicos podem deletar.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        historyService.deleteHistory(id);
    }
}

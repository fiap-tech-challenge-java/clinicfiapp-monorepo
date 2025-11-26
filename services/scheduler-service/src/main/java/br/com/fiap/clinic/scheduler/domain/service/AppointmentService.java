package br.com.fiap.clinic.scheduler.domain.service;

import br.com.fiap.clinic.scheduler.domain.entity.Appointment;
import br.com.fiap.clinic.scheduler.domain.entity.AppointmentStatus;
import br.com.fiap.clinic.scheduler.domain.entity.Doctor;
import br.com.fiap.clinic.scheduler.domain.entity.Patient;
import br.com.fiap.clinic.scheduler.domain.repository.AppointmentRepository;
import br.com.fiap.clinic.scheduler.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de gerenciamento de consultas.
 * <p>
 * Responsável pelas operações de CRUD e regras de negócio relacionadas às consultas médicas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    /**
     * Busca todas as consultas cadastradas.
     *
     * @return lista de todas as consultas
     */
    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    /**
     * Busca todas as consultas com paginação.
     *
     * @param pageable configuração de paginação
     * @return página de consultas
     */
    public Page<Appointment> findAll(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }

    /**
     * Busca uma consulta por ID.
     *
     * @param id ID da consulta
     * @return consulta encontrada
     * @throws ResourceNotFoundException se a consulta não for encontrada
     */
    public Appointment findById(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada com ID: " + id));
    }

    /**
     * Busca consultas para envio de lembretes.
     * Retorna consultas agendadas entre o período especificado que estão ativas
     * e com status CONFIRMADO ou SOLICITADO.
     *
     * @param startDate data inicial do período
     * @param endDate data final do período
     * @return lista de consultas para lembrete
     */
    public List<Appointment> findAppointmentsForReminder(LocalDateTime startDate, LocalDateTime endDate) {
        return appointmentRepository.findAppointmentsForReminder(startDate, endDate);
    }

    /**
     * Cria uma nova consulta.
     * Valida se o médico e paciente existem e estão ativos.
     *
     * @param appointment dados da consulta a ser criada
     * @return consulta criada
     * @throws ResourceNotFoundException se médico ou paciente não existirem
     * @throws IllegalArgumentException se médico ou paciente estiverem inativos
     */
    @Transactional
    public Appointment create(Appointment appointment) {
        // Validar médico
        Doctor doctor = doctorService.findById(appointment.getDoctor().getId());
        if (!doctor.getIsActive()) {
            throw new IllegalArgumentException("Médico está inativo e não pode receber consultas");
        }

        // Validar paciente
        Patient patient = patientService.findById(appointment.getPatient().getId());
        if (!patient.getIsActive()) {
            throw new IllegalArgumentException("Paciente está inativo e não pode agendar consultas");
        }

        // Validar datas
        if (appointment.getStartAt().isAfter(appointment.getEndAt())) {
            throw new IllegalArgumentException("Data de início deve ser anterior à data de fim");
        }

        if (appointment.getStartAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Não é possível agendar consultas no passado");
        }

        // Definir status padrão se não informado
        if (appointment.getStatus() == null) {
            appointment.setStatus(AppointmentStatus.SOLICITADO);
        }

        return appointmentRepository.save(appointment);
    }

    /**
     * Atualiza os dados de uma consulta existente.
     *
     * @param id ID da consulta a ser atualizada
     * @param appointmentDetails novos dados da consulta
     * @return consulta atualizada
     * @throws ResourceNotFoundException se a consulta não for encontrada
     */
    @Transactional
    public Appointment update(UUID id, Appointment appointmentDetails) {
        Appointment appointment = findById(id);

        // Não permite alterar consultas já realizadas ou canceladas
        if (appointment.getStatus() == AppointmentStatus.REALIZADO ||
            appointment.getStatus() == AppointmentStatus.CANCELADO) {
            throw new IllegalStateException("Não é possível alterar consultas realizadas ou canceladas");
        }

        appointment.setStartAt(appointmentDetails.getStartAt());
        appointment.setEndAt(appointmentDetails.getEndAt());
        appointment.setStatus(appointmentDetails.getStatus());

        return appointmentRepository.save(appointment);
    }

    /**
     * Confirma uma consulta.
     *
     * @param id ID da consulta a ser confirmada
     * @return consulta confirmada
     * @throws ResourceNotFoundException se a consulta não for encontrada
     * @throws IllegalStateException se a consulta não puder ser confirmada
     */
    @Transactional
    public Appointment confirm(UUID id) {
        Appointment appointment = findById(id);

        if (appointment.getStatus() != AppointmentStatus.SOLICITADO) {
            throw new IllegalStateException("Apenas consultas com status SOLICITADO podem ser confirmadas");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMADO);
        return appointmentRepository.save(appointment);
    }

    /**
     * Cancela uma consulta.
     *
     * @param id ID da consulta a ser cancelada
     * @return consulta cancelada
     * @throws ResourceNotFoundException se a consulta não for encontrada
     * @throws IllegalStateException se a consulta não puder ser cancelada
     */
    @Transactional
    public Appointment cancel(UUID id) {
        Appointment appointment = findById(id);

        if (appointment.getStatus() == AppointmentStatus.REALIZADO) {
            throw new IllegalStateException("Não é possível cancelar consultas já realizadas");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELADO) {
            throw new IllegalStateException("Consulta já está cancelada");
        }

        appointment.setStatus(AppointmentStatus.CANCELADO);
        return appointmentRepository.save(appointment);
    }

    /**
     * Marca uma consulta como realizada.
     *
     * @param id ID da consulta a ser marcada como realizada
     * @return consulta atualizada
     * @throws ResourceNotFoundException se a consulta não for encontrada
     * @throws IllegalStateException se a consulta não puder ser marcada como realizada
     */
    @Transactional
    public Appointment markAsCompleted(UUID id) {
        Appointment appointment = findById(id);

        if (appointment.getStatus() == AppointmentStatus.CANCELADO) {
            throw new IllegalStateException("Não é possível marcar como realizada uma consulta cancelada");
        }

        if (appointment.getStatus() == AppointmentStatus.REALIZADO) {
            throw new IllegalStateException("Consulta já está marcada como realizada");
        }

        appointment.setStatus(AppointmentStatus.REALIZADO);
        return appointmentRepository.save(appointment);
    }

    /**
     * Desativa uma consulta (soft delete).
     *
     * @param id ID da consulta a ser desativada
     * @throws ResourceNotFoundException se a consulta não for encontrada
     */
    @Transactional
    public void deactivate(UUID id) {
        Appointment appointment = findById(id);
        appointment.setActive(false);
        appointmentRepository.save(appointment);
    }

    /**
     * Ativa uma consulta.
     *
     * @param id ID da consulta a ser ativada
     * @throws ResourceNotFoundException se a consulta não for encontrada
     */
    @Transactional
    public void activate(UUID id) {
        Appointment appointment = findById(id);
        appointment.setActive(true);
        appointmentRepository.save(appointment);
    }

    /**
     * Remove permanentemente uma consulta.
     *
     * @param id ID da consulta a ser removida
     * @throws ResourceNotFoundException se a consulta não for encontrada
     */
    @Transactional
    public void delete(UUID id) {
        if (!appointmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Consulta não encontrada com ID: " + id);
        }
        appointmentRepository.deleteById(id);
    }

    /**
     * Verifica se uma consulta existe.
     *
     * @param id ID da consulta
     * @return true se a consulta existe, false caso contrário
     */
    public boolean exists(UUID id) {
        return appointmentRepository.existsById(id);
    }
}

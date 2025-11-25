package br.com.fiap.clinic.scheduler.job;

import br.com.fiap.clinic.scheduler.domain.service.AppointmentReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job agendado para enviar lembretes diários de consultas
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyAppointmentReminderJob {
    
    private final AppointmentReminderService reminderService;
    
    /**
     * Executa todos os dias às 8h da manhã
     * Envia lembretes para as consultas do dia seguinte
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyReminders() {
        log.info("=== Iniciando job de lembretes diários de consultas ===");
        
        try {
            reminderService.sendDailyReminders();
            log.info("=== Job de lembretes diários concluído com sucesso ===");
        } catch (Exception e) {
            log.error("=== Erro ao executar job de lembretes diários ===", e);
        }
    }
}


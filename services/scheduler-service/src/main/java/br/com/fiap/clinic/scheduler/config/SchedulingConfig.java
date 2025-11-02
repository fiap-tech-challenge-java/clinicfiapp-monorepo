package br.com.fiap.clinic.scheduler.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuração para habilitar o agendamento de tarefas (ex: Outbox Relay).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
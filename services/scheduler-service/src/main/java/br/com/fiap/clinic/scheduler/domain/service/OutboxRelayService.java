package br.com.fiap.clinic.scheduler.domain.service;

import br.com.fiap.clinic.scheduler.config.KafkaConfig;
import br.com.fiap.clinic.scheduler.domain.entity.OutboxEvent;
import br.com.fiap.clinic.scheduler.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${outbox.batch.size:50}")
    private int batchSize;

    private static final String DEFAULT_TOPIC = KafkaConfig.TOPIC_NAME;

    @Scheduled(initialDelayString = "15000", fixedDelayString = "${outbox.poll.delay:5000}")
    @SchedulerLock(name = "OutboxRelay_pollAndRelayEvents",
            lockAtLeastFor = "2s",
            lockAtMostFor = "30s")
    @Transactional
    public void pollAndRelayEvents() {
        log.debug("Iniciando poll para eventos do Outbox...");
        Pageable batch = PageRequest.of(0, batchSize);
        List<OutboxEvent> events = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc(batch);

        if (events.isEmpty()) {
            log.debug("Nenhum evento encontrado.");
            return;
        }

        log.info("Encontrados {} eventos para retransmitir.", events.size());

        for (OutboxEvent event : events) {
            try {
                // Lógica simples de roteamento de tópico
                // Idealmente, isso viria de uma configuração ou do próprio evento
                String topic = DEFAULT_TOPIC;

                // Usamos o aggregateId como chave do Kafka para garantir particionamento e ordem
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());

                // Marca como processado
                event.setProcessed(true);
                log.debug("Evento {} enviado ao tópico {}", event.getId(), topic);

            } catch (Exception e) {
                // Se falhar o envio ao Kafka, o @Transactional fará rollback
                // e o evento não será marcado como 'processed', sendo tentado novamente no próximo poll.
                log.error("Falha ao enviar evento {} ao Kafka. Erro: {}", event.getId(), e.getMessage());
                // Lança a exceção para garantir o rollback transacional
                throw new RuntimeException("Falha no relay do Kafka, rollback será executado.", e);
            }
        }

        // Salva as alterações (marcando como 'processed')
        outboxEventRepository.saveAll(events);
        log.info("Relay de {} eventos concluído com sucesso.", events.size());
    }
}
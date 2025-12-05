package br.com.fiap.clinic.notification.config;

import br.com.fiap.clinic.notification.domain.dto.AppointmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, AppointmentEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Commit manual para garantir processamento

        JsonDeserializer<AppointmentEvent> jsonDeserializer =
                new JsonDeserializer<>(AppointmentEvent.class);
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                jsonDeserializer
        );
    }

    @Bean
    public ProducerFactory<String, AppointmentEvent> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, AppointmentEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Configuração de ErrorHandler com Retry e Backoff Exponencial
     * - Tenta reprocessar 3 vezes com intervalo de 2 minutos
     * - Se falhar todas as tentativas, envia para DLT
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, AppointmentEvent> kafkaTemplate) {
        // Backoff fixo de 2 minutos entre tentativas, máximo 3 tentativas
        FixedBackOff fixedBackOff = new FixedBackOff(120000L, 2L); // 2 minutos, 2 retries (total 3 tentativas)

        DefaultErrorHandler errorHandler = new DefaultErrorHandler((consumerRecord, exception) -> {
            // Este método é chamado quando todas as tentativas falharam
            log.error("🚨 DEAD LETTER TOPIC: Todas as tentativas falharam para o evento. " +
                    "Enviando para DLT. Offset: {}, Partition: {}, Error: {}",
                    consumerRecord.offset(), consumerRecord.partition(), exception.getMessage());

            // Envia para tópico DLT
            try {
                kafkaTemplate.send("appointment-events-dlt", (AppointmentEvent) consumerRecord.value());
                log.info("✅ Mensagem enviada para DLT com sucesso");
            } catch (Exception e) {
                log.error("❌ ERRO CRÍTICO: Falha ao enviar para DLT: {}", e.getMessage(), e);
            }
        }, fixedBackOff);

        // Log de cada tentativa de retry
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("⚠️ RETRY: Tentativa {} de reprocessamento após erro: {}",
                    deliveryAttempt, ex.getMessage());
        });

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AppointmentEvent>
    kafkaListenerContainerFactory(DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, AppointmentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler); // Configura o error handler com retry
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.RECORD
        ); // ACK por mensagem processada com sucesso

        return factory;
    }
}

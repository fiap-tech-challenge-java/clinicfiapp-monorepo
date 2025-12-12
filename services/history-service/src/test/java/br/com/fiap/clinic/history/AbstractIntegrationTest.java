package br.com.fiap.clinic.history;

import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    static {
        postgres.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("api.security.token.public-key", () -> "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuCObYVoT9YAI/o90Eanf3Um4vXd6FfNz8HARr7Mwr9YrfkiVCZSA6tzvivqN/iZPlUfs2+SrDR3X/fVaKgVudTL910no3eFI3g6yTib1yOto+f3N6wXDlUyaBDbUcJ1izDRURgbzLeVPqZ9/cmrZ3RaDzTeivblo5u8yQPiF+q/QuI56VKZeAjqn23HsQuypfVs9wah7qlVkBIt8Y8IcyomPUuC+rmy9ik3O9+68a6DzupgV7Djgy+HMpCLREpB09D0KgtwmfsHtjGLNv/O0j4UtdEB3K53L19iq5WULEPQzMZwKcQ6BlfZVpdtu9JlOxCYjlDGygoaxMv7liQ0M+QIDAQAB\n-----END PUBLIC KEY-----");
    }
}


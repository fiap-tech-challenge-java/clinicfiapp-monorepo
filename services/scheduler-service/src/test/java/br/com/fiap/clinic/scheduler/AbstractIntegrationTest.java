package br.com.fiap.clinic.scheduler;

import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureGraphQlTester
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
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

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("api.security.token.private-key", () -> "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC4I5thWhP1gAj+j3QRqd/dSbi9d3oV83PwcBGvszCv1it+SJUJlIDq3O+K6o3+Jk+VR+zb5KsNHdf99VoqBW51Mv3XSejd4UjeDrJOJvXI62j5/c3rBcOVTJoENtRwnWLMNFRGBvMt5U+pn39yatndFoPNN6K9uWjm7zJA+IX6r9C4jnpUpl4COqfbcexC7Kl9Wz3BqHuqVWQEi3xjwhzKiY9S4L6ubL2KTc737rxroPO6mBXsOODL4cykItESkHT0PQqC3CZ+we2MYs2/87SPhS10QHcrncvX2KrlZQsQ9DMxnApxDoGV9lWl2270mU7EJiOUMbKChrEy/uWJDQz5AgMBAAECggEAMebQZn8TVp42kcoNkfXf6affbVUyHD3IRzoPyPY+NQDM86G25gw9LajnL9Xl2iDGw3Vk/qiHJHxYewXhQ+8J4rAJc60fcvXpGeiQsLZFOIh3f/wksFL1EIJcYNT9GHHGgwGRbxLWFzOENbs4Pikv1ocR++zeaR0UjTT4gPv4zxkcRe6JyFeLiJnpDCdLvMHUTz+UlntLVeA6LdfhDzG4uOacJ/lR3CZc2cTsKM3VX687nFYs2NeDxHcTF0d0dSlKcGmRp49K7qznAdNIauUztvdvXkkJcD7wE6DssqtI/+tip2RT/ce0bjTJc7w0EAdLYmmj+SrcdF89uCHKLuzw+wKBgQC5S871zpcwAt0MtKqqvFW7QJJpEJ6F9+U2ZOlS1S25Pezzl2w0fVomav+kvtXThDVa7k0QFuek2yyOMayujtSkkggNQTmIQv134Y0dBRmSv3YF93sBeZsVZgUGjB/2p5HFIn2J5VmoIJLWRL3qUcsXKeUz/fI3hbNZN7kwbNg2pwKBgQD+Zsa24Xfc+0Iqts4tHzfy08Im/MlFtsTJo8ip3pDuQr4MSj96HtmgS36nmZs9GmiHljFzHrrYEN4vpWKv2Xbvaf9lpApZP5xzb+REQTQNYroOaxKBsOGBXw1uJLskd0Yn4WXutAbcs/bFOrT56YXRH4hNIR9DaxF7LNeJYdazXwKBgAm0Fpp4vyIchEbuDyHxyuuLbh9iZ2rMmIxIrI9R6psQyx/WhiwZhGcOo2SVOc1h1wl0AUUebfMMJ0ErCMf7YtdbbmCDUef42h01CpbCdZxhumSFmUNygtSUCx9Upzfbfp68N6bJA/DyA76Eyw1CS0hjQUV/vvd5YPMDIAeRK6FxAoGBAJmbTvLPssMQRwRpepYOGocZS0qlKZZZY/roVNoUk+f5Dq13C1reU8MQsnnaKry7PFBZ5KPA80pQ7QdR0gOhx8mJc/dYFBONcBbXgbmgsSA/812PUw5R0ed+kCpoLUM4bXjZBOYbd2U9mIvABdQ8J1t8sZz4mdroRBbenYnLNIr7AoGAa3njRXuxkdaAauxIXBHtdiidpMZjtU4TiZDN+okNDGPGmS0sVI1gMB0H5ng0ZTT8ORM1lxfCgbhgU7QLfh9sghvG7oaagXzHcgJKSLElcDTkDbDUaqQQH7wrU+Xu4qt38/r2EubZtLMuVy5Mu6wbu3pScdkmI7PsxVpGGPbNTzY=\n");
    }
}
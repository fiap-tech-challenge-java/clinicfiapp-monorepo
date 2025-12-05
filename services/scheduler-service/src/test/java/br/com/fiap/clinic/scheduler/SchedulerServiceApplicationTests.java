package br.com.fiap.clinic.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

@DisplayName("Teste de Carga de Contexto (Sanity Check)")
class SchedulerServiceApplicationTests extends AbstractIntegrationTest {

	@Test
	@DisplayName("O contexto da aplicação deve subir sem erros")
	void contextLoads() {
	}

	public static class KeyGen {
		public static void main(String[] args) throws Exception {
			// 1. Gera o par de chaves RSA 2048 bits
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair kp = kpg.generateKeyPair();

			// 2. Codifica para Base64 (formato texto)
			String priv = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
			String pub = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

			// 3. Imprime no formato para colar no docker-compose.yml
			System.out.println("\n--- COPIE E SALVE AS CHAVES ABAIXO ---");
			System.out.println("JWT_PRIVATE_KEY=" + priv);
			System.out.println("JWT_PUBLIC_KEY=" + pub);
			System.out.println("--------------------------------------\n");
		}
	}
}

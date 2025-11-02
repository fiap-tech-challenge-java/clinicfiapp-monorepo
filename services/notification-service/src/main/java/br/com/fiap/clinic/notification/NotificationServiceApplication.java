package br.com.fiap.clinic.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class NotificationServiceApplication {

	private static final Logger log = LoggerFactory.getLogger(NotificationServiceApplication.class);

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(NotificationServiceApplication.class, args);
		Environment env = ctx.getEnvironment();
		String port = env.getProperty("local.server.port", env.getProperty("server.port", "8080"));
		String profiles = String.join(",", env.getActiveProfiles());
		if (profiles.isEmpty()) {
			profiles = "default";
		}
		log.info("🔔  {} started — http://localhost:{} — profiles=[{}]", "Notification Service", port, profiles);
	}

}

package br.com.fiap.clinic.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class SchedulerServiceApplication {

	private static final Logger log = LoggerFactory.getLogger(SchedulerServiceApplication.class);

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(SchedulerServiceApplication.class, args);
		Environment env = ctx.getEnvironment();
		String port = env.getProperty("local.server.port", env.getProperty("server.port", "8080"));
		String profiles = String.join(",", env.getActiveProfiles());
		if (profiles.isEmpty()) {
			profiles = "default";
		}
		// Log with an icon to make it clear the service started
		log.info("🚀  {} started — http://localhost:{} — profiles=[{}]", "Scheduler Service", port, profiles);
	}

}

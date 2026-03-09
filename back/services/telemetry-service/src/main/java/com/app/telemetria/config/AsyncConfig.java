package com.app.telemetria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
	
	@Bean(name = "alertaTaskExecutor")
	public Executor alertaTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);  // Threads mínimas
		executor.setMaxPoolSize(8); // Threads máximas
		executor.setQueueCapacity(200); // Capacidade da fila
		executor.setThreadNamePrefix("AlertaAsync-");
		executor.initialize();
		return executor;
	}

}

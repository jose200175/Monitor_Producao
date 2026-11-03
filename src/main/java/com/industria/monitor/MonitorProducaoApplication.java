package com.industria.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@EnableJpaAuditing 
@SpringBootApplication
public class MonitorProducaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonitorProducaoApplication.class, args);
	}

}

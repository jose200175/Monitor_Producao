package com.industria.monitor.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.industria.monitor.model.Trabalhador;
import com.industria.monitor.repository.TrabalhadorRepository;

@Configuration
@Profile("dev")
public class TrabalhadorDevInitializer {

	private static final String CODIGO_ADMIN_TESTE = "ADMIN-TESTE";

	@Bean
	ApplicationRunner criarAdministradorDeDesenvolvimento(TrabalhadorRepository trabalhadorRepository) {
		return args -> trabalhadorRepository.findByCodigo(CODIGO_ADMIN_TESTE).orElseGet(() -> {
			Trabalhador trabalhador = new Trabalhador();
			trabalhador.setCodigo(CODIGO_ADMIN_TESTE);
			trabalhador.setNome("Administrador de desenvolvimento");
			return trabalhadorRepository.save(trabalhador);
		});
	}
}

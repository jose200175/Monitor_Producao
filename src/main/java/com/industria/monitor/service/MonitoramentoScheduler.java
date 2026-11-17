package com.industria.monitor.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.industria.monitor.repository.MaquinaRepository;

@Component
public class MonitoramentoScheduler {

	private final MaquinaRepository maquinaRepository;
	private final MonitoramentoAusenciaService ausenciaService;

	public MonitoramentoScheduler(
			MaquinaRepository maquinaRepository,
			MonitoramentoAusenciaService ausenciaService) {
		this.maquinaRepository = maquinaRepository;
		this.ausenciaService = ausenciaService;
	}

	@Scheduled(fixedDelay = 10000)
	public void verificarAusenciaDeDados() {
		LocalDateTime agora = LocalDateTime.now();
		maquinaRepository.findAll().forEach(maquina -> ausenciaService.atualizarEstado(maquina, agora));
	}
}

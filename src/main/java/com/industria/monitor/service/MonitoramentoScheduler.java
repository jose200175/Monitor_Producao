package com.industria.monitor.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.industria.monitor.repository.MaquinaRepository;

@Component
public class MonitoramentoScheduler {

	private final MaquinaRepository maquinaRepository;
	private final MonitoramentoAusenciaService ausenciaService;
	private final LeituraService leituraService;

	public MonitoramentoScheduler(
			MaquinaRepository maquinaRepository,
			MonitoramentoAusenciaService ausenciaService,
			LeituraService leituraService) {
		this.maquinaRepository = maquinaRepository;
		this.ausenciaService = ausenciaService;
		this.leituraService = leituraService;
	}

	@Scheduled(fixedDelay = 10000)
	public void verificarAusenciaDeDados() {
		LocalDateTime agora = LocalDateTime.now();
		maquinaRepository.findAll().forEach(maquina -> {
			ausenciaService.atualizarEstado(maquina, agora);
			leituraService.verificarAlertasDeProducao(maquina.getId());
		});
	}
}

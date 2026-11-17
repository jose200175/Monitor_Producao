package com.industria.monitor.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.industria.monitor.model.Maquina;
import com.industria.monitor.model.enums.TipoRegra;
import com.industria.monitor.repository.LeituraRepository;

@Service
public class MonitoramentoAusenciaService {

	private static final Duration LIMITE_AUSENCIA = Duration.ofSeconds(60);

	private final LeituraRepository leituraRepository;
	private final AlertaService alertaService;

	public MonitoramentoAusenciaService(LeituraRepository leituraRepository, AlertaService alertaService) {
		this.leituraRepository = leituraRepository;
		this.alertaService = alertaService;
	}

	@Transactional
	public boolean atualizarEstado(Maquina maquina, LocalDateTime agora) {
		LocalDateTime ultimaRecepcao = leituraRepository
				.findTopByMaquinaIdOrderByCriadoEmDesc(maquina.getId())
				.map(leitura -> leitura.getCriadoEm())
				.orElse(maquina.getCriadoEm());
		boolean semDados = Duration.between(ultimaRecepcao, agora).compareTo(LIMITE_AUSENCIA) > 0;

		if (semDados) {
			alertaService.abrirSeNecessario(maquina, TipoRegra.AUSENCIA_DADOS);
		} else {
			alertaService.resolverAtivos(maquina, TipoRegra.AUSENCIA_DADOS);
		}
		return semDados;
	}
}

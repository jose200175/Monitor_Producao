package com.industria.monitor.service;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.industria.monitor.model.Maquina;
import com.industria.monitor.model.enums.TipoRegra;
import com.industria.monitor.repository.MaquinaRepository;

@Service
public class MonitoramentoManutencaoService {

	private final MaquinaRepository maquinaRepository;
	private final AlertaService alertaService;

	public MonitoramentoManutencaoService(
			MaquinaRepository maquinaRepository, AlertaService alertaService) {
		this.maquinaRepository = maquinaRepository;
		this.alertaService = alertaService;
	}

	@Scheduled(fixedDelay = 60000)
	@Transactional
	public void verificarManutencoesProgramadas() {
		LocalDate hoje = LocalDate.now();
		for (Maquina maquina : maquinaRepository.findAll()) {
			LocalDate dataManutencao = maquina.getManutencaoProgramadaEm();
			if (dataManutencao == null) {
				continue;
			}
			LocalDate inicioAviso = dataManutencao.minusDays(
					maquina.getAntecedenciaManutencaoDias() == null
							? 7 : maquina.getAntecedenciaManutencaoDias());
			if (!hoje.isBefore(inicioAviso)) {
				alertaService.abrirSeNecessario(maquina, TipoRegra.MANUTENCAO_PROGRAMADA);
			}
		}
	}
}

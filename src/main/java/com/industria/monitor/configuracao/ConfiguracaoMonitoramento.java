package com.industria.monitor.configuracao;

import java.util.List;

public record ConfiguracaoMonitoramento(
		int janelaIndicadoresSegundos,
		int limiteAusenciaSegundos,
		List<ConfiguracaoMaquina> maquinas) {

	public static final int VERSAO_ATUAL = 1;

	public ConfiguracaoMonitoramento {
		if (janelaIndicadoresSegundos <= 0) {
			throw new IllegalArgumentException("A janela de indicadores deve ser maior que zero.");
		}
		if (limiteAusenciaSegundos <= 0) {
			throw new IllegalArgumentException("O limite de ausência deve ser maior que zero.");
		}
		maquinas = maquinas == null ? List.of() : List.copyOf(maquinas);
	}
}

package com.industria.monitor.importacao;

import java.util.List;

import com.industria.monitor.model.Leitura;

public record ResultadoImportacao(
		List<Leitura> leiturasImportadas,
		List<LinhaRejeitada> linhasRejeitadas) {

	public int getQuantidadeImportada() {
		return leiturasImportadas.size();
	}

	public int getQuantidadeRejeitada() {
		return linhasRejeitadas.size();
	}
}

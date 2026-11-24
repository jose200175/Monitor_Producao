package com.industria.monitor.configuracao;

import java.math.BigDecimal;

public record ConfiguracaoMaquina(
		Long maquinaId,
		String nome,
		String unidade,
		BigDecimal limiteMinimo,
		BigDecimal intervaloSegundos) {
}

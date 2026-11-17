package com.industria.monitor.dto.maquina;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.industria.monitor.model.enums.StatusProducao;

public record IndicadorProducaoResponseDTO(
		Long maquinaId,
		LocalDateTime inicioJanela,
		LocalDateTime fimJanela,
		BigDecimal quantidadeProduzida,
		BigDecimal limiteMinimo,
		String unidade,
		StatusProducao status
) {}

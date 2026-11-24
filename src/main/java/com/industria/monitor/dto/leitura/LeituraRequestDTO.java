package com.industria.monitor.dto.leitura;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeituraRequestDTO(
	    Long maquinaId,
	   	LocalDateTime inicioIntervalo,
	    LocalDateTime fimIntervalo,
	    BigDecimal quantidade,
	    String unidade,
	    String chaveEvento,
	    String codigoResponsavelEdicao
) {
	public LeituraRequestDTO(
			Long maquinaId,
			LocalDateTime inicioIntervalo,
			LocalDateTime fimIntervalo,
			BigDecimal quantidade,
			String unidade,
			String chaveEvento) {
		this(maquinaId, inicioIntervalo, fimIntervalo, quantidade, unidade, chaveEvento, null);
	}

	public LeituraRequestDTO(
			Long maquinaId,
			LocalDateTime inicioIntervalo,
			LocalDateTime fimIntervalo,
			BigDecimal quantidade,
			String unidade) {
		this(maquinaId, inicioIntervalo, fimIntervalo, quantidade, unidade, null, null);
	}
}
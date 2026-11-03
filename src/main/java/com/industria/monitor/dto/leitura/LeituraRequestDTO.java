package com.industria.monitor.dto.leitura;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeituraRequestDTO(
	    Long maquinaId,
	   	LocalDateTime inicioIntervalo,
	    LocalDateTime fimIntervalo,
	    BigDecimal quantidade,
	    String unidade
	) {}
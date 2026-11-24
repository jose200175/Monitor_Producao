package com.industria.monitor.dto.configuracao;

import jakarta.validation.constraints.Min;

public record ConfiguracaoMonitoramentoDTO(
		@Min(value = 1, message = "A janela de indicadores deve ser maior que zero")
		Integer janelaIndicadoresSegundos) {
}

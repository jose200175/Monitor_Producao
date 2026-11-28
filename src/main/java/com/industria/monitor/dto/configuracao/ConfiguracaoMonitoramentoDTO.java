package com.industria.monitor.dto.configuracao;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfiguracaoMonitoramentoDTO(
		@NotNull(message = "A janela de indicadores é obrigatória")
		@Min(value = 1, message = "A janela de indicadores deve ser maior que zero")
		Integer janelaIndicadoresSegundos) {
}

package com.industria.monitor.dto.alerta;

import com.industria.monitor.model.enums.AcaoAlerta;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record AcaoAlertaRequestDTO(
		@NotNull AcaoAlerta acao,
		@NotBlank(message = "Informe o código do trabalhador responsável") String codigoTrabalhador,
		String observacoes) {
	public AcaoAlertaRequestDTO(AcaoAlerta acao) {
		this(acao, null, null);
	}
}

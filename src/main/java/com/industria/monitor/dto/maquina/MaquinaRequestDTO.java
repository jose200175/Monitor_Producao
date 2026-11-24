package com.industria.monitor.dto.maquina;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaquinaRequestDTO(

    @NotBlank(message = "O nome da máquina é obrigatório")
    String nome,

    @NotBlank(message = "A unidade de produção é obrigatória")
    String unidade,

    @NotNull(message = "O limite mínimo é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal limiteMin,

    @Min(value = 1, message = "O intervalo deve ser maior que zero")
    BigDecimal intervaloSeg,

    @DecimalMin(value = "0.0", inclusive = false, message = "O limite de superprodução deve ser maior que zero")
    BigDecimal limiteSuperproducao,

    LocalDate manutencaoProgramadaEm,

    @Min(value = 0, message = "A antecedência da manutenção não pode ser negativa")
    Integer antecedenciaManutencaoDias,

    String codigoResponsavelEdicao

) {
	public MaquinaRequestDTO(
			String nome,
			String unidade,
			BigDecimal limiteMin,
			BigDecimal intervaloSeg) {
		this(nome, unidade, limiteMin, intervaloSeg, null, null, 7, null);
	}

	public MaquinaRequestDTO(
			String nome,
			String unidade,
			BigDecimal limiteMin,
			BigDecimal intervaloSeg,
			BigDecimal limiteSuperproducao,
			LocalDate manutencaoProgramadaEm,
			Integer antecedenciaManutencaoDias) {
		this(nome, unidade, limiteMin, intervaloSeg, limiteSuperproducao,
				manutencaoProgramadaEm, antecedenciaManutencaoDias, null);
	}
}
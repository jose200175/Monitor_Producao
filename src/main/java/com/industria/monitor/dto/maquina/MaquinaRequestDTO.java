package com.industria.monitor.dto.maquina;

import java.math.BigDecimal;

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
    BigDecimal intervaloSeg

) {}
package com.industria.monitor.dto.maquina;

import java.math.BigDecimal;

import com.industria.monitor.model.Maquina;

public record MaquinaResponseDTO(
    Long id,
    String nome,
    String unidade,
    BigDecimal limiteMin,
    BigDecimal intervaloSeg
) {
    public static MaquinaResponseDTO fromEntity(Maquina maquina) {
        return new MaquinaResponseDTO(
            maquina.getId(),
            maquina.getNome(),
            maquina.getUnidade(),
            maquina.getLimiteMin(),
            maquina.getIntervaloSeg()
        );
    }
}
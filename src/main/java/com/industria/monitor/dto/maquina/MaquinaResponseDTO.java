package com.industria.monitor.dto.maquina;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.industria.monitor.model.Maquina;

public record MaquinaResponseDTO(
    Long id,
    String nome,
    String unidade,
    BigDecimal limiteMin,
    BigDecimal intervaloSeg,
    BigDecimal limiteSuperproducao,
    LocalDate manutencaoProgramadaEm,
    Integer antecedenciaManutencaoDias,
    String codigoResponsavelEdicao
) {
	public MaquinaResponseDTO(
			Long id, String nome, String unidade, BigDecimal limiteMin, BigDecimal intervaloSeg) {
		this(id, nome, unidade, limiteMin, intervaloSeg, null, null, 7, null);
	}

    public static MaquinaResponseDTO fromEntity(Maquina maquina) {
        return new MaquinaResponseDTO(
            maquina.getId(),
            maquina.getNome(),
            maquina.getUnidade(),
            maquina.getLimiteMin(),
            maquina.getIntervaloSeg(),
            maquina.getLimiteSuperproducao(),
            maquina.getManutencaoProgramadaEm(),
            maquina.getAntecedenciaManutencaoDias(),
            maquina.getResponsavelEdicao() == null ? null : maquina.getResponsavelEdicao().getCodigo()
        );
    }
}
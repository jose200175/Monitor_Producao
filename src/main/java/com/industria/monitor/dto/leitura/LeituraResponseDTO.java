package com.industria.monitor.dto.leitura;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.industria.monitor.model.Leitura;

public record LeituraResponseDTO(
    Long id,
    Long maquinaId,
    LocalDateTime inicioIntervalo,
    LocalDateTime fimIntervalo,
    BigDecimal quantidade,
    String unidade,
    String chaveEvento,
    LocalDateTime criadoEm,
    String codigoResponsavelEdicao
) {
	public LeituraResponseDTO(
			Long id,
			Long maquinaId,
			LocalDateTime inicioIntervalo,
			LocalDateTime fimIntervalo,
			BigDecimal quantidade,
			String unidade,
			String chaveEvento,
			LocalDateTime criadoEm) {
		this(id, maquinaId, inicioIntervalo, fimIntervalo, quantidade,
				unidade, chaveEvento, criadoEm, null);
	}

    public static LeituraResponseDTO fromEntity(Leitura leitura) {
        return new LeituraResponseDTO(
            leitura.getId(),
            leitura.getMaquina().getId(),
            leitura.getInicioIntervalo(),
            leitura.getFimIntervalo(),
            leitura.getQuantidade(),
            leitura.getUnidade(),
            leitura.getChaveEvento(),
            leitura.getCriadoEm(),
            leitura.getResponsavelEdicao() == null ? null : leitura.getResponsavelEdicao().getCodigo()
        );
    }
}
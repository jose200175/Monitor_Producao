package com.industria.monitor.dto.alerta;

import java.time.LocalDateTime;

import com.industria.monitor.model.Alerta;
import com.industria.monitor.model.enums.EstadoAlerta;
import com.industria.monitor.model.enums.TipoRegra;

public record AlertaResponseDTO(
    Long id,
    Long maquinaId,
    TipoRegra tipoRegra,
    EstadoAlerta estado,
    LocalDateTime criadoEm,
    String codigoResponsavel,
    String nomeResponsavel,
    String observacoes
) {
	public AlertaResponseDTO(
			Long id, Long maquinaId, TipoRegra tipoRegra, EstadoAlerta estado, LocalDateTime criadoEm) {
		this(id, maquinaId, tipoRegra, estado, criadoEm, null, null, null);
	}

	public static AlertaResponseDTO fromEntity(Alerta alerta) {
	    return new AlertaResponseDTO(
	        alerta.getId(),
	        alerta.getMaquina().getId(),
	        alerta.getTipoRegra(),
	        alerta.getEstado(),
	        alerta.getCriadoEm(),
	        alerta.getResponsavel() == null ? null : alerta.getResponsavel().getCodigo(),
	        alerta.getResponsavel() == null ? null : alerta.getResponsavel().getNome(),
	        alerta.getObservacoes()
	    );
	}
}
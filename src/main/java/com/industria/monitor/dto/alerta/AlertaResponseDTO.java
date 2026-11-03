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
    LocalDateTime criadoEm
) {
	public static AlertaResponseDTO fromEntity(Alerta alerta) {
	    return new AlertaResponseDTO(
	        alerta.getId(),
	        alerta.getMaquina().getId(),
	        alerta.getTipoRegra(),
	        alerta.getEstado(),
	        alerta.getCriadoEm()
	    );
	}
}
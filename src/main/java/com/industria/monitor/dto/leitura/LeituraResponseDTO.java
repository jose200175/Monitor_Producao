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
    LocalDateTime criadoEm
) {
    public static LeituraResponseDTO fromEntity(Leitura leitura) {
        return new LeituraResponseDTO(
            leitura.getId(),
            leitura.getMaquina().getId(),
            leitura.getInicioIntervalo(),
            leitura.getFimIntervalo(),
            leitura.getQuantidade(),
            leitura.getUnidade(),
            leitura.getCriadoEm()
        );
    }
}
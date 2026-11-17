package com.industria.monitor.dto.alerta;

import com.industria.monitor.model.enums.AcaoAlerta;

import jakarta.validation.constraints.NotNull;

public record AcaoAlertaRequestDTO(@NotNull AcaoAlerta acao) {}

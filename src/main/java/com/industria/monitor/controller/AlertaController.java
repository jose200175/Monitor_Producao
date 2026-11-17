package com.industria.monitor.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.industria.monitor.dto.alerta.AcaoAlertaRequestDTO;
import com.industria.monitor.dto.alerta.AlertaResponseDTO;
import com.industria.monitor.service.AlertaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/alertas")
public class AlertaController {

	private final AlertaService service;

	public AlertaController(AlertaService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<List<AlertaResponseDTO>> listar() {
		return ResponseEntity.ok(service.listar());
	}

	@PatchMapping("/{alertaId}")
	public ResponseEntity<AlertaResponseDTO> aplicarAcao(
			@PathVariable Long alertaId,
			@RequestBody @Valid AcaoAlertaRequestDTO dto) {
		return ResponseEntity.ok(service.aplicarAcao(alertaId, dto.acao()));
	}
}

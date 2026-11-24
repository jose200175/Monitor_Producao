package com.industria.monitor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.industria.monitor.configuracao.ConfiguracaoMonitoramentoRuntime;
import com.industria.monitor.dto.configuracao.ConfiguracaoMonitoramentoDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/configuracoes")
public class ConfiguracaoController {

	private final ConfiguracaoMonitoramentoRuntime configuracao;

	public ConfiguracaoController(ConfiguracaoMonitoramentoRuntime configuracao) {
		this.configuracao = configuracao;
	}

	@GetMapping
	public ConfiguracaoMonitoramentoDTO buscar() {
		return new ConfiguracaoMonitoramentoDTO(
				configuracao.getJanelaIndicadoresSegundos());
	}

	@PutMapping
	public ResponseEntity<ConfiguracaoMonitoramentoDTO> alterar(
			@RequestBody @Valid ConfiguracaoMonitoramentoDTO dto) {
		configuracao.setJanelaIndicadoresSegundos(dto.janelaIndicadoresSegundos());
		return ResponseEntity.ok(buscar());
	}
}

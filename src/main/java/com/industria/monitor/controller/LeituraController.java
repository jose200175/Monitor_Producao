package com.industria.monitor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.industria.monitor.dto.leitura.LeituraRequestDTO;
import com.industria.monitor.dto.leitura.LeituraResponseDTO;
import com.industria.monitor.service.LeituraService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/leituras")
public class LeituraController {
	
	private LeituraService service;
	
	public LeituraController(LeituraService service) {
		this.service = service;
	}
	
	@PostMapping
	public ResponseEntity<LeituraResponseDTO> cadastrarLeitura(@RequestBody @Valid LeituraRequestDTO dto){
		LeituraResponseDTO leituraSalva = service.cadastrarLeitura(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(leituraSalva);
	}
	
	@GetMapping("/maquina/{maquinaId}")
	public ResponseEntity<List<LeituraResponseDTO>> buscarLeiturasMaquina(@PathVariable  Long maquinaId){
		return ResponseEntity.ok(service.findByMaquinaId(maquinaId));
	}
}

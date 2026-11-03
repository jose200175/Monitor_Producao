package com.industria.monitor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.industria.monitor.dto.maquina.MaquinaRequestDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;
import com.industria.monitor.service.MaquinaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/maquinas")
public class MaquinaController {
	
	private MaquinaService service;
	
	public MaquinaController(MaquinaService maquinaService) {
		this.service = maquinaService;
	}

	@PostMapping
	public ResponseEntity<MaquinaResponseDTO> cadastrarMaquina(@RequestBody @Valid MaquinaRequestDTO dto){
		MaquinaResponseDTO maquinaSalva = service.criarMaquina(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(maquinaSalva);
	}
	
	@GetMapping
	public ResponseEntity<List<MaquinaResponseDTO>> findAll(){
		return ResponseEntity.ok(service.findAll());
	}
}

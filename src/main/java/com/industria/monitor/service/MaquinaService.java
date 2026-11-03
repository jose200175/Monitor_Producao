package com.industria.monitor.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.industria.monitor.dto.maquina.MaquinaRequestDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;
import com.industria.monitor.model.Maquina;
import com.industria.monitor.repository.MaquinaRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@Service
public class MaquinaService {
	private final MaquinaRepository repository;
	
	public MaquinaService(MaquinaRepository maquinaRepository) {
		this.repository = maquinaRepository;
	}

	@Transactional
	public MaquinaResponseDTO criarMaquina(@Valid MaquinaRequestDTO dto) {
		Maquina maquina = new Maquina();
		maquina.setNome(dto.nome());
		maquina.setIntervaloSeg(dto.intervaloSeg());
		maquina.setLimiteMin(dto.limiteMin());
		maquina.setUnidade(dto.unidade());
		
		Maquina maquinaSalva = repository.save(maquina);
		return MaquinaResponseDTO.fromEntity(maquinaSalva);
		
	}
	
	public List<MaquinaResponseDTO> findAll(){
		return repository.findAll()
				.stream()
				.map(MaquinaResponseDTO::fromEntity)
                .toList();
	}
}

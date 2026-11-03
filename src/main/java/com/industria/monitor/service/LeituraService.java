package com.industria.monitor.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.industria.monitor.dto.leitura.LeituraRequestDTO;
import com.industria.monitor.dto.leitura.LeituraResponseDTO;
import com.industria.monitor.model.Leitura;
import com.industria.monitor.model.Maquina;
import com.industria.monitor.repository.LeituraRepository;
import com.industria.monitor.repository.MaquinaRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@Service
public class LeituraService {
	
	private LeituraRepository repository;
	private MaquinaRepository maquinaRepository;
	
	public LeituraService (MaquinaRepository maquinaRepository, LeituraRepository repository) {
		this.repository = repository;
		this.maquinaRepository = maquinaRepository;
	}

	@Transactional
	public LeituraResponseDTO cadastrarLeitura(@Valid LeituraRequestDTO dto) {
		
		boolean existe = repository.existsByMaquinaIdAndInicioIntervaloAndFimIntervalo(dto.maquinaId(), dto.inicioIntervalo(), dto.inicioIntervalo());
		if (existe){
			throw new RuntimeException("Leitura ja registrada para essa maquina nesse periodo");
		}
		Leitura leitura = new Leitura();
		Maquina maquina = maquinaRepository.findById(dto.maquinaId()).orElseThrow(() ->new RuntimeException("Maquina não encontrada"));
		leitura.setMaquina(maquina);
		leitura.setInicioIntervalo(dto.inicioIntervalo());
		leitura.setFimIntervalo(dto.fimIntervalo());
		leitura.setQuantidade(dto.quantidade());
		leitura.setUnidade(dto.unidade());
		
		Leitura leituraSalva = repository.save(leitura);
		return LeituraResponseDTO.fromEntity(leituraSalva);
	}
	
	public List<LeituraResponseDTO> findByMaquinaId(Long maquinaId){
	    return repository.findByMaquinaId(maquinaId)
	            .orElse(List.of()) // Tira do Optional com segurança
	            .stream()
	            .map(LeituraResponseDTO::fromEntity)
	            .toList();
	}	
}

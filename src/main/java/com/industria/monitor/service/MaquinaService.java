package com.industria.monitor.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.industria.monitor.dto.maquina.MaquinaRequestDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;
import com.industria.monitor.model.Maquina;
import com.industria.monitor.repository.MaquinaRepository;
import com.industria.monitor.model.Trabalhador;
import com.industria.monitor.repository.TrabalhadorRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@Service
public class MaquinaService {
	private static final BigDecimal INTERVALO_PADRAO_SEG = new BigDecimal("60");

	private final MaquinaRepository repository;
	private final TrabalhadorRepository trabalhadorRepository;
	
	public MaquinaService(MaquinaRepository maquinaRepository, TrabalhadorRepository trabalhadorRepository) {
		this.repository = maquinaRepository;
		this.trabalhadorRepository = trabalhadorRepository;
	}

	@Transactional
	public MaquinaResponseDTO criarMaquina(@Valid MaquinaRequestDTO dto) {
		exigirDados(dto);
		Maquina maquina = new Maquina();
		maquina.setNome(dto.nome());
		maquina.setIntervaloSeg(intervaloOuPadrao(dto));
		maquina.setLimiteMin(dto.limiteMin());
		maquina.setUnidade(dto.unidade());
		aplicarConfiguracoes(maquina, dto);
		
		Maquina maquinaSalva = repository.save(maquina);
		return MaquinaResponseDTO.fromEntity(maquinaSalva);
		
	}

	@Transactional
	public MaquinaResponseDTO atualizar(Long maquinaId, @Valid MaquinaRequestDTO dto) {
		exigirDados(dto);
		Maquina maquina = repository.findById(maquinaId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Máquina não encontrada"));
		if (dto.codigoResponsavelEdicao() == null || dto.codigoResponsavelEdicao().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Informe o código do trabalhador responsável pela edição");
		}
		Trabalhador responsavel = trabalhadorRepository.findByCodigo(dto.codigoResponsavelEdicao().trim())
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.BAD_REQUEST, "Código de trabalhador não cadastrado"));

		maquina.setNome(dto.nome());
		maquina.setUnidade(dto.unidade());
		maquina.setLimiteMin(dto.limiteMin());
		maquina.setIntervaloSeg(intervaloOuPadrao(dto));
		aplicarConfiguracoes(maquina, dto);
		maquina.setResponsavelEdicao(responsavel);
		return MaquinaResponseDTO.fromEntity(repository.save(maquina));
	}

	private void exigirDados(MaquinaRequestDTO dto) {
		if (dto == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados da máquina não informados");
		}
	}

	private BigDecimal intervaloOuPadrao(MaquinaRequestDTO dto) {
		return dto.intervaloSeg() == null ? INTERVALO_PADRAO_SEG : dto.intervaloSeg();
	}

	private void aplicarConfiguracoes(Maquina maquina, MaquinaRequestDTO dto) {
		maquina.setLimiteSuperproducao(dto.limiteSuperproducao());
		maquina.setManutencaoProgramadaEm(dto.manutencaoProgramadaEm());
		maquina.setAntecedenciaManutencaoDias(
				dto.antecedenciaManutencaoDias() == null ? 7 : dto.antecedenciaManutencaoDias());
	}
	
	public List<MaquinaResponseDTO> findAll(){
		return repository.findAll()
				.stream()
				.map(MaquinaResponseDTO::fromEntity)
                .toList();
	}
}

package com.industria.monitor.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.industria.monitor.dto.alerta.AlertaResponseDTO;
import com.industria.monitor.model.Alerta;
import com.industria.monitor.model.Maquina;
import com.industria.monitor.model.Trabalhador;
import com.industria.monitor.model.enums.AcaoAlerta;
import com.industria.monitor.model.enums.EstadoAlerta;
import com.industria.monitor.model.enums.TipoRegra;
import com.industria.monitor.repository.AlertaRepository;
import com.industria.monitor.repository.TrabalhadorRepository;

@Service
public class AlertaService {

	private final AlertaRepository repository;
	private final TrabalhadorRepository trabalhadorRepository;

	public AlertaService(AlertaRepository repository, TrabalhadorRepository trabalhadorRepository) {
		this.repository = repository;
		this.trabalhadorRepository = trabalhadorRepository;
	}

	@Transactional
	public void abrirSeNecessario(Maquina maquina, TipoRegra tipoRegra) {
		List<Alerta> ativos = repository.findByMaquinaAndTipoRegraAndEstadoNot(
				maquina, tipoRegra, EstadoAlerta.RESOLVIDO);
		if (!ativos.isEmpty()) {
			return;
		}

		Alerta alerta = new Alerta();
		alerta.setMaquina(maquina);
		alerta.setTipoRegra(tipoRegra);
		alerta.setEstado(EstadoAlerta.ABERTO);
		repository.save(alerta);
	}

	@Transactional(readOnly = true)
	public List<AlertaResponseDTO> listar() {
		return repository.findAll().stream()
				.map(AlertaResponseDTO::fromEntity)
				.toList();
	}

	@Transactional
	public AlertaResponseDTO aplicarAcao(
			Long alertaId, AcaoAlerta acao, String codigoTrabalhador, String observacoes) {
		Alerta alerta = repository.findById(alertaId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerta não encontrado"));
		if (codigoTrabalhador == null || codigoTrabalhador.isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "Informe o código do trabalhador responsável");
		}
		Trabalhador responsavel = trabalhadorRepository.findByCodigo(codigoTrabalhador.trim())
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.BAD_REQUEST, "Código de trabalhador não cadastrado"));

		if (acao == AcaoAlerta.RECONHECER) {
			if (alerta.getEstado() != EstadoAlerta.ABERTO) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Apenas alertas abertos podem ser reconhecidos");
			}
			alerta.setEstado(EstadoAlerta.RECONHECIDO);
		} else if (acao == AcaoAlerta.COLOCAR_EM_MANUTENCAO) {
			exigirObservacoes(observacoes);
			if (alerta.getEstado() == EstadoAlerta.RESOLVIDO) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alerta já está resolvido");
			}
			alerta.setEstado(EstadoAlerta.EM_MANUTENCAO);
		} else if (acao == AcaoAlerta.RESOLVER) {
			exigirObservacoes(observacoes);
			if (alerta.getEstado() == EstadoAlerta.RESOLVIDO) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alerta já está resolvido");
			}
			alerta.setEstado(EstadoAlerta.RESOLVIDO);
		}
		alerta.setResponsavel(responsavel);
		alerta.setObservacoes(observacoes == null || observacoes.isBlank() ? null : observacoes.trim());

		return AlertaResponseDTO.fromEntity(alerta);
	}

	private void exigirObservacoes(String observacoes) {
		if (observacoes == null || observacoes.isBlank()) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "Informe as observações da intervenção");
		}
	}
}

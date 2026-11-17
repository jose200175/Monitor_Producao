package com.industria.monitor.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.industria.monitor.dto.alerta.AlertaResponseDTO;
import com.industria.monitor.model.Alerta;
import com.industria.monitor.model.Maquina;
import com.industria.monitor.model.enums.AcaoAlerta;
import com.industria.monitor.model.enums.EstadoAlerta;
import com.industria.monitor.model.enums.TipoRegra;
import com.industria.monitor.repository.AlertaRepository;

@Service
public class AlertaService {

	private final AlertaRepository repository;

	public AlertaService(AlertaRepository repository) {
		this.repository = repository;
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

	@Transactional
	public void resolverAtivos(Maquina maquina, TipoRegra tipoRegra) {
		List<Alerta> ativos = repository.findByMaquinaAndTipoRegraAndEstadoNot(
				maquina, tipoRegra, EstadoAlerta.RESOLVIDO);
		ativos.forEach(alerta -> alerta.setEstado(EstadoAlerta.RESOLVIDO));
	}

	@Transactional(readOnly = true)
	public List<AlertaResponseDTO> listar() {
		return repository.findAll().stream()
				.map(AlertaResponseDTO::fromEntity)
				.toList();
	}

	@Transactional
	public AlertaResponseDTO aplicarAcao(Long alertaId, AcaoAlerta acao) {
		Alerta alerta = repository.findById(alertaId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerta não encontrado"));

		if (acao == AcaoAlerta.RECONHECER) {
			if (alerta.getEstado() != EstadoAlerta.ABERTO) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Apenas alertas abertos podem ser reconhecidos");
			}
			alerta.setEstado(EstadoAlerta.RECONHECIDO);
		} else if (acao == AcaoAlerta.RESOLVER) {
			if (alerta.getEstado() == EstadoAlerta.RESOLVIDO) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alerta já está resolvido");
			}
			alerta.setEstado(EstadoAlerta.RESOLVIDO);
		}

		return AlertaResponseDTO.fromEntity(alerta);
	}
}

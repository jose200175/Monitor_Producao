package com.industria.monitor.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.industria.monitor.dto.leitura.LeituraRequestDTO;
import com.industria.monitor.dto.leitura.LeituraResponseDTO;
import com.industria.monitor.dto.maquina.IndicadorProducaoResponseDTO;
import com.industria.monitor.configuracao.ConfiguracaoMonitoramentoRuntime;
import com.industria.monitor.model.Leitura;
import com.industria.monitor.model.Maquina;
import com.industria.monitor.model.Trabalhador;
import com.industria.monitor.model.enums.StatusProducao;
import com.industria.monitor.model.enums.TipoRegra;
import com.industria.monitor.repository.LeituraRepository;
import com.industria.monitor.repository.MaquinaRepository;
import com.industria.monitor.repository.TrabalhadorRepository;

import jakarta.validation.Valid;

@Service
public class LeituraService {

	private final LeituraRepository repository;
	private final MaquinaRepository maquinaRepository;
	private final AlertaService alertaService;
	private final MonitoramentoAusenciaService ausenciaService;
	private final ConfiguracaoMonitoramentoRuntime configuracao;
	private final TrabalhadorRepository trabalhadorRepository;

	public LeituraService(
			MaquinaRepository maquinaRepository,
			LeituraRepository repository,
			AlertaService alertaService,
			MonitoramentoAusenciaService ausenciaService,
			ConfiguracaoMonitoramentoRuntime configuracao,
			TrabalhadorRepository trabalhadorRepository) {
		this.repository = repository;
		this.maquinaRepository = maquinaRepository;
		this.alertaService = alertaService;
		this.ausenciaService = ausenciaService;
		this.configuracao = configuracao;
		this.trabalhadorRepository = trabalhadorRepository;
	}

	@Transactional
	public LeituraResponseDTO cadastrarLeitura(@Valid LeituraRequestDTO dto) {
		validarDados(dto);

		boolean existe = repository.findByMaquinaId(dto.maquinaId())
				.orElse(List.of())
				.stream()
				.anyMatch(leituraExistente ->
						leituraExistente.getInicioIntervalo().equals(dto.inicioIntervalo())
								&& leituraExistente.getFimIntervalo().equals(dto.fimIntervalo()));
		if (existe) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Leitura já registrada para essa máquina nesse período");
		}
		if (dto.chaveEvento() != null && repository.existsByChaveEvento(dto.chaveEvento())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Chave de evento já utilizada");
		}

		Maquina maquina = maquinaRepository.findById(dto.maquinaId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Máquina não encontrada"));
		Leitura leitura = new Leitura();
		leitura.setMaquina(maquina);
		leitura.setInicioIntervalo(dto.inicioIntervalo());
		leitura.setFimIntervalo(dto.fimIntervalo());
		leitura.setQuantidade(dto.quantidade());
		leitura.setUnidade(dto.unidade());
		leitura.setChaveEvento(dto.chaveEvento() == null ? UUID.randomUUID().toString() : dto.chaveEvento());

		Leitura leituraSalva = repository.save(leitura);
		LocalDateTime agora = LocalDateTime.now();
		ausenciaService.atualizarEstado(maquina, agora);
		atualizarAlertasDeProducao(maquina, calcularQuantidade(maquina, agora));
		return LeituraResponseDTO.fromEntity(leituraSalva);
	}

	@Transactional
	public LeituraResponseDTO atualizar(Long leituraId, @Valid LeituraRequestDTO dto) {
		validarDados(dto);
		if (dto.codigoResponsavelEdicao() == null || dto.codigoResponsavelEdicao().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Informe o código do trabalhador responsável pela edição");
		}
		Leitura leitura = repository.findById(leituraId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leitura não encontrada"));
		boolean periodoDuplicado = repository.findByMaquinaId(dto.maquinaId())
				.orElse(List.of())
				.stream()
				.anyMatch(outra -> !outra.getId().equals(leituraId)
						&& outra.getInicioIntervalo().equals(dto.inicioIntervalo())
						&& outra.getFimIntervalo().equals(dto.fimIntervalo()));
		if (periodoDuplicado) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Já existe uma leitura para essa máquina nesse período");
		}
		String chaveEvento = dto.chaveEvento() == null ? leitura.getChaveEvento() : dto.chaveEvento();
		if (repository.existsByChaveEventoAndIdNot(chaveEvento, leituraId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Chave de evento já utilizada");
		}

		Maquina maquina = maquinaRepository.findById(dto.maquinaId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Máquina não encontrada"));
		Trabalhador responsavel = trabalhadorRepository.findByCodigo(dto.codigoResponsavelEdicao().trim())
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.BAD_REQUEST, "Código de trabalhador não cadastrado"));
		leitura.setMaquina(maquina);
		leitura.setInicioIntervalo(dto.inicioIntervalo());
		leitura.setFimIntervalo(dto.fimIntervalo());
		leitura.setQuantidade(dto.quantidade());
		leitura.setUnidade(dto.unidade());
		leitura.setChaveEvento(chaveEvento);
		leitura.setResponsavelEdicao(responsavel);
		return LeituraResponseDTO.fromEntity(repository.save(leitura));
	}

	private void validarDados(LeituraRequestDTO dto) {
		if (dto == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados da leitura inválidos");
		}
		if (dto.maquinaId() == null || dto.inicioIntervalo() == null || dto.fimIntervalo() == null
				|| !dto.inicioIntervalo().isBefore(dto.fimIntervalo())
				|| dto.quantidade() == null || dto.quantidade().signum() < 0
				|| dto.unidade() == null || dto.unidade().isBlank()
				|| (dto.chaveEvento() != null
						&& (dto.chaveEvento().isBlank() || dto.chaveEvento().length() > 64))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados da leitura inválidos");
		}
	}

	@Transactional(readOnly = true)
	public List<LeituraResponseDTO> findByMaquinaId(Long maquinaId) {
		return repository.findByMaquinaId(maquinaId)
				.orElse(List.of())
				.stream()
				.map(LeituraResponseDTO::fromEntity)
				.toList();
	}

	@Transactional
	public IndicadorProducaoResponseDTO buscarIndicadores(Long maquinaId) {
		Maquina maquina = maquinaRepository.findById(maquinaId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Máquina não encontrada"));
		LocalDateTime agora = LocalDateTime.now();
		boolean semDados = ausenciaService.atualizarEstado(maquina, agora);
		BigDecimal quantidade = calcularQuantidade(maquina, agora);
		atualizarAlertasDeProducao(maquina, quantidade);

		StatusProducao status = semDados
				? StatusProducao.SEM_DADOS
				: quantidade.compareTo(maquina.getLimiteMin()) < 0
						? StatusProducao.BAIXA
						: StatusProducao.NORMAL;
		return new IndicadorProducaoResponseDTO(
				maquina.getId(),
				agora.minus(configuracao.getJanelaIndicadores()),
				agora,
				quantidade,
				maquina.getLimiteMin(),
				maquina.getUnidade(),
				status);
	}

	@Transactional
	public void verificarAlertasDeProducao(Long maquinaId) {
		Maquina maquina = maquinaRepository.findById(maquinaId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Máquina não encontrada"));
		atualizarAlertasDeProducao(maquina, calcularQuantidade(maquina, LocalDateTime.now()));
	}

	private void atualizarAlertasDeProducao(Maquina maquina, BigDecimal quantidade) {
		atualizarAlertaBaixaProducao(maquina, quantidade);
		BigDecimal limiteSuperproducao = maquina.getLimiteSuperproducao();
		if (limiteSuperproducao != null && quantidade.compareTo(limiteSuperproducao) > 0) {
			alertaService.abrirSeNecessario(maquina, TipoRegra.SUPERPRODUCAO);
		}
	}

	private BigDecimal calcularQuantidade(Maquina maquina, LocalDateTime agora) {
		LocalDateTime inicioJanela = agora.minus(configuracao.getJanelaIndicadores());
		List<Leitura> leituras = repository.findByMaquinaIdAndFimIntervaloAfterAndInicioIntervaloBefore(
				maquina.getId(), inicioJanela, agora);

		BigDecimal total = BigDecimal.ZERO;
		for (Leitura leitura : leituras) {
			LocalDateTime inicioSobreposicao = leitura.getInicioIntervalo().isAfter(inicioJanela)
					? leitura.getInicioIntervalo() : inicioJanela;
			LocalDateTime fimSobreposicao = leitura.getFimIntervalo().isBefore(agora)
					? leitura.getFimIntervalo() : agora;
			Duration duracaoLeitura = Duration.between(leitura.getInicioIntervalo(), leitura.getFimIntervalo());
			Duration duracaoSobreposta = Duration.between(inicioSobreposicao, fimSobreposicao);
			if (duracaoLeitura.isZero() || duracaoLeitura.isNegative() || duracaoSobreposta.isNegative()) {
				continue;
			}

			BigDecimal proporcao = segundos(duracaoSobreposta)
					.divide(segundos(duracaoLeitura), MathContext.DECIMAL128);
			total = total.add(leitura.getQuantidade().multiply(proporcao));
		}
		return total;
	}

	private BigDecimal segundos(Duration duracao) {
		return BigDecimal.valueOf(duracao.getSeconds())
				.add(BigDecimal.valueOf(duracao.getNano(), 9));
	}

	private void atualizarAlertaBaixaProducao(Maquina maquina, BigDecimal quantidade) {
		if (quantidade.compareTo(maquina.getLimiteMin()) < 0) {
			alertaService.abrirSeNecessario(maquina, TipoRegra.LIMITE_MINIMO);
		}
	}
}

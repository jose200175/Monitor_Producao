package com.industria.monitor.simulacao;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.industria.monitor.client.MonitorApiClient;
import com.industria.monitor.dto.leitura.LeituraRequestDTO;
import com.industria.monitor.dto.maquina.MaquinaRequestDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;

public class SimuladorMaquina implements AutoCloseable {

	public static final int TOTAL_LEITURAS_SINTETICAS = 100;
	public static final long INTERVALO_ENVIO_SEGUNDOS = 60;
	public static final long SEMENTE = 2026L;
	private static final BigDecimal LIMITE_MINIMO = BigDecimal.TEN;
	private static final String UNIDADE = "peças/min";
	private static final Logger LOGGER = Logger.getLogger(SimuladorMaquina.class.getName());

	private final MonitorApiClient api;
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
		Thread thread = new Thread(task, "simulador-maquina");
		thread.setDaemon(true);
		return thread;
	});
	private final Random randomAoVivo = new Random(SEMENTE);
	private volatile boolean executando;
	private ScheduledFuture<?> tarefa;

	public SimuladorMaquina(MonitorApiClient api) {
		this.api = api;
	}

	public synchronized Map<CenarioSimulacao, MaquinaResponseDTO> garantirMaquinas()
			throws IOException, InterruptedException {
		Map<CenarioSimulacao, MaquinaResponseDTO> maquinas = new EnumMap<>(CenarioSimulacao.class);
		List<MaquinaResponseDTO> cadastradas = api.listarMaquinas();
		for (CenarioSimulacao cenario : CenarioSimulacao.values()) {
			MaquinaResponseDTO maquina = null;
			for (MaquinaResponseDTO cadastrada : cadastradas) {
				if (cadastrada.nome().equals(cenario.nomeMaquina())) {
					maquina = cadastrada;
					break;
				}
			}
			if (maquina == null) {
				maquina = api.cadastrarMaquina(new MaquinaRequestDTO(
						cenario.nomeMaquina(),
						UNIDADE,
						LIMITE_MINIMO,
						BigDecimal.valueOf(INTERVALO_ENVIO_SEGUNDOS)));
			}
			maquinas.put(cenario, maquina);
		}
		return Map.copyOf(maquinas);
	}

	public List<LeituraSimulada> gerarConjuntoDe100(Map<CenarioSimulacao, MaquinaResponseDTO> maquinas) {
		validarMaquinas(maquinas);
		LocalDateTime referencia = LocalDateTime.now().withNano(0);
		Random random = new Random(SEMENTE);
		List<LeituraSimulada> leituras = new ArrayList<>(TOTAL_LEITURAS_SINTETICAS);
		int sequencia = 1;

		for (CenarioSimulacao cenario : CenarioSimulacao.values()) {
			MaquinaResponseDTO maquina = maquinas.get(cenario);
			int quantidadeDoCenario = cenario == CenarioSimulacao.PRODUCAO_NORMAL ? 34 : 33;
			for (int indice = 0; indice < quantidadeDoCenario; indice++) {
				LocalDateTime fim = referencia.minusSeconds(
						(long) (quantidadeDoCenario - indice - 1) * INTERVALO_ENVIO_SEGUNDOS);
				BigDecimal quantidade = quantidadeSintetica(cenario, maquina.limiteMin(), random);
				leituras.add(new LeituraSimulada(
						String.format("SIM-%06d", sequencia++),
						maquina.id(),
						cenario,
						fim.minusSeconds(INTERVALO_ENVIO_SEGUNDOS),
						fim,
						quantidade,
						maquina.unidade()));
			}
		}
		leituras.sort(Comparator.comparing(LeituraSimulada::inicioIntervalo)
				.thenComparing(LeituraSimulada::maquinaId));
		return List.copyOf(leituras);
	}

	public Path exportarConjuntoDe100(Map<CenarioSimulacao, MaquinaResponseDTO> maquinas, Path arquivo)
			throws IOException {
		List<LeituraSimulada> leituras = gerarConjuntoDe100(maquinas);
		Path destino = arquivo.toAbsolutePath().normalize();
		Path diretorio = destino.getParent();
		if (diretorio != null) {
			Files.createDirectories(diretorio);
		}

		List<String> linhas = new ArrayList<>(leituras.size() + 1);
		linhas.add("chave_evento,maquina_id,cenario,inicio_intervalo,fim_intervalo,quantidade,unidade");
		leituras.stream()
				.map(LeituraSimulada::toCsv)
				.forEach(linhas::add);
		Files.write(destino, linhas, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		return destino;
	}

	public synchronized void iniciar(
			Map<CenarioSimulacao, MaquinaResponseDTO> maquinas,
			Consumer<String> aoEnviar,
			Consumer<Exception> aoFalhar) {
		validarMaquinas(maquinas);
		if (executando) {
			throw new IllegalStateException("O simulador já está em execução");
		}
		executando = true;
		tarefa = executor.scheduleAtFixedRate(
				() -> enviarCiclo(maquinas, aoEnviar, aoFalhar),
				0,
				INTERVALO_ENVIO_SEGUNDOS,
				TimeUnit.SECONDS);
	}

	public synchronized void parar() {
		executando = false;
		if (tarefa != null) {
			tarefa.cancel(true);
			tarefa = null;
		}
	}

	public boolean isExecutando() {
		return executando;
	}

	private void enviarCiclo(
			Map<CenarioSimulacao, MaquinaResponseDTO> maquinas,
			Consumer<String> aoEnviar,
			Consumer<Exception> aoFalhar) {
		Exception falha = null;
		for (CenarioSimulacao cenario : List.of(
				CenarioSimulacao.PRODUCAO_NORMAL,
				CenarioSimulacao.BAIXA_PRODUCAO)) {
			if (!executando || Thread.currentThread().isInterrupted()) {
				return;
			}
			try {
				LeituraSimulada leitura = gerarLeituraAoVivo(cenario, maquinas.get(cenario));
				api.cadastrarLeitura(leitura.toRequest());
				aoEnviar.accept("Leitura " + leitura.chaveEvento() + " enviada para "
						+ maquinas.get(cenario).nome());
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				return;
			} catch (IOException exception) {
				LOGGER.log(Level.WARNING, "Falha ao enviar leitura simulada do cenário " + cenario, exception);
				if (falha == null) {
					falha = exception;
				} else {
					falha.addSuppressed(exception);
				}
			}
		}
		if (falha != null) {
			aoFalhar.accept(falha);
		}
	}

	private synchronized LeituraSimulada gerarLeituraAoVivo(
			CenarioSimulacao cenario, MaquinaResponseDTO maquina) {
		LocalDateTime fim = LocalDateTime.now().withNano(0);
		String chaveEvento = "LIVE-" + cenario.name() + "-" + UUID.randomUUID();
		return new LeituraSimulada(
				chaveEvento,
				maquina.id(),
				cenario,
				fim.minusSeconds(INTERVALO_ENVIO_SEGUNDOS),
				fim,
				quantidadeSintetica(cenario, maquina.limiteMin(), randomAoVivo),
				maquina.unidade());
	}

	private BigDecimal quantidadeSintetica(
			CenarioSimulacao cenario, BigDecimal limiteMinimo, Random random) {
		if (cenario == CenarioSimulacao.BAIXA_PRODUCAO) {
			return limiteMinimo.divide(BigDecimal.valueOf(4));
		}
		return limiteMinimo.add(BigDecimal.valueOf(5 + random.nextInt(6)));
	}

	private void validarMaquinas(Map<CenarioSimulacao, MaquinaResponseDTO> maquinas) {
		for (CenarioSimulacao cenario : CenarioSimulacao.values()) {
			MaquinaResponseDTO maquina = maquinas.get(cenario);
			if (maquina == null || maquina.id() == null || maquina.limiteMin() == null
					|| maquina.unidade() == null || maquina.unidade().isBlank()) {
				throw new IllegalArgumentException("Máquina inválida para o cenário " + cenario);
			}
		}
	}

	@Override
	public void close() {
		parar();
		executor.shutdownNow();
	}

	public record LeituraSimulada(
			String chaveEvento,
			Long maquinaId,
			CenarioSimulacao cenario,
			LocalDateTime inicioIntervalo,
			LocalDateTime fimIntervalo,
			BigDecimal quantidade,
			String unidade) {

		public LeituraRequestDTO toRequest() {
			return new LeituraRequestDTO(
					maquinaId, inicioIntervalo, fimIntervalo, quantidade, unidade, chaveEvento);
		}

		private String toCsv() {
			return String.join(",",
					escaparCsv(chaveEvento),
					maquinaId.toString(),
					cenario.name(),
					inicioIntervalo.toString(),
					fimIntervalo.toString(),
					quantidade.toPlainString(),
					escaparCsv(unidade));
		}

		private static String escaparCsv(String valor) {
			return "\"" + valor.replace("\"", "\"\"") + "\"";
		}
	}
}

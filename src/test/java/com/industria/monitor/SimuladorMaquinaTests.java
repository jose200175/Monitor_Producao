package com.industria.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.industria.monitor.client.MonitorApiClient;
import com.industria.monitor.dto.leitura.LeituraRequestDTO;
import com.industria.monitor.dto.maquina.MaquinaRequestDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;
import com.industria.monitor.simulacao.CenarioSimulacao;
import com.industria.monitor.simulacao.SimuladorMaquina;

class SimuladorMaquinaTests {

	@TempDir
	Path tempDir;

	@Test
	void geraEExportaCemLeiturasComOsTresCenariosEChavesUnicas() throws IOException {
		try (SimuladorMaquina simulador = new SimuladorMaquina(new MonitorApiClient("http://localhost:8080"))) {
			Map<CenarioSimulacao, MaquinaResponseDTO> maquinas = maquinasDeTeste();
			List<SimuladorMaquina.LeituraSimulada> leituras = simulador.gerarConjuntoDe100(maquinas);
			Path csv = simulador.exportarConjuntoDe100(maquinas, tempDir.resolve("leituras.csv"));

			assertEquals(100, leituras.size());
			assertEquals(100, leituras.stream().map(SimuladorMaquina.LeituraSimulada::chaveEvento).distinct().count());
			assertEquals(Set.of(CenarioSimulacao.values()), leituras.stream()
					.map(SimuladorMaquina.LeituraSimulada::cenario).collect(Collectors.toSet()));
			assertEquals(34, leituras.stream()
					.filter(leitura -> leitura.cenario() == CenarioSimulacao.PRODUCAO_NORMAL).count());
			assertEquals(33, leituras.stream()
					.filter(leitura -> leitura.cenario() == CenarioSimulacao.BAIXA_PRODUCAO).count());
			assertEquals(33, leituras.stream()
					.filter(leitura -> leitura.cenario() == CenarioSimulacao.AUSENCIA_ENVIO).count());
			assertTrue(leituras.stream().allMatch(leitura ->
					Duration.between(leitura.inicioIntervalo(), leitura.fimIntervalo())
							.getSeconds() == SimuladorMaquina.INTERVALO_ENVIO_SEGUNDOS));
			assertTrue(leituras.stream().filter(leitura ->
					leitura.cenario() == CenarioSimulacao.BAIXA_PRODUCAO)
					.allMatch(leitura -> leitura.quantidade().compareTo(new BigDecimal("10")) < 0));
			assertTrue(leituras.stream().filter(leitura ->
					leitura.cenario() == CenarioSimulacao.PRODUCAO_NORMAL)
					.allMatch(leitura -> leitura.quantidade().compareTo(new BigDecimal("10")) >= 0));

			List<String> linhas = Files.readAllLines(csv);
			assertEquals(101, linhas.size());
			assertTrue(linhas.get(0).contains("chave_evento,maquina_id,cenario"));
		}
	}

	@Test
	void garanteCadastroDasTresMaquinasSemDuplicarAoRepetir() throws Exception {
		FakeMonitorApiClient api = new FakeMonitorApiClient();
		try (SimuladorMaquina simulador = new SimuladorMaquina(api)) {
			Map<CenarioSimulacao, MaquinaResponseDTO> primeiras = simulador.garantirMaquinas();
			Map<CenarioSimulacao, MaquinaResponseDTO> seguintes = simulador.garantirMaquinas();

			assertEquals(3, primeiras.size());
			assertEquals(3, seguintes.size());
			assertEquals(3, api.maquinas.size());
			assertEquals(Set.of(CenarioSimulacao.values()), seguintes.keySet());
		}
	}

	@Test
	void enviaCenariosNormaisEDeBaixaProducaoACadaSessentaSegundosSemEnviarCenarioDeAusencia()
			throws InterruptedException {
		CountDownLatch primeirasLeiturasEnviadas = new CountDownLatch(2);
		FakeMonitorApiClient api = new FakeMonitorApiClient(primeirasLeiturasEnviadas);
		try (SimuladorMaquina simulador = new SimuladorMaquina(api)) {
			simulador.iniciar(maquinasDeTeste(), ignored -> {}, exception -> {});

			assertTrue(primeirasLeiturasEnviadas.await(5, TimeUnit.SECONDS));
			simulador.parar();
		}

		assertEquals(2, api.leiturasEnviadas.size());
		assertTrue(api.leiturasEnviadas.stream().allMatch(leitura ->
				Duration.between(leitura.inicioIntervalo(), leitura.fimIntervalo())
						.getSeconds() == SimuladorMaquina.INTERVALO_ENVIO_SEGUNDOS));
		assertTrue(api.leiturasEnviadas.stream().noneMatch(leitura ->
				leitura.maquinaId().equals(3L)));
		assertFalse(api.leiturasEnviadas.stream().anyMatch(leitura ->
				leitura.chaveEvento() == null || leitura.chaveEvento().isBlank()));
		assertEquals(2, api.leiturasEnviadas.stream()
				.map(LeituraRequestDTO::chaveEvento).distinct().count());
		assertEquals(60, SimuladorMaquina.INTERVALO_ENVIO_SEGUNDOS);
	}

	private Map<CenarioSimulacao, MaquinaResponseDTO> maquinasDeTeste() {
		Map<CenarioSimulacao, MaquinaResponseDTO> maquinas = new EnumMap<>(CenarioSimulacao.class);
		long id = 1;
		for (CenarioSimulacao cenario : CenarioSimulacao.values()) {
			maquinas.put(cenario, new MaquinaResponseDTO(
					id++, cenario.nomeMaquina(), "peças/min", BigDecimal.TEN, BigDecimal.valueOf(60)));
		}
		return Map.copyOf(maquinas);
	}

	private static class FakeMonitorApiClient extends MonitorApiClient {
		private final List<LeituraRequestDTO> leiturasEnviadas = new CopyOnWriteArrayList<>();
		private final CountDownLatch primeirasLeiturasEnviadas;
		private final Map<String, MaquinaResponseDTO> maquinas = new ConcurrentHashMap<>();
		private final AtomicLong proximoId = new AtomicLong();

		private FakeMonitorApiClient() {
			this(new CountDownLatch(0));
		}

		private FakeMonitorApiClient(CountDownLatch primeirasLeiturasEnviadas) {
			super("http://localhost:8080");
			this.primeirasLeiturasEnviadas = primeirasLeiturasEnviadas;
		}

		@Override
		public List<MaquinaResponseDTO> listarMaquinas() {
			return List.copyOf(maquinas.values());
		}

		@Override
		public MaquinaResponseDTO cadastrarMaquina(MaquinaRequestDTO maquina) {
			return maquinas.computeIfAbsent(maquina.nome(), ignored -> new MaquinaResponseDTO(
					proximoId.incrementAndGet(),
					maquina.nome(),
					maquina.unidade(),
					maquina.limiteMin(),
					maquina.intervaloSeg()));
		}

		@Override
		public void cadastrarLeitura(LeituraRequestDTO leitura) throws IOException {
			leiturasEnviadas.add(leitura);
			primeirasLeiturasEnviadas.countDown();
		}
	}
}

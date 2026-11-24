package com.industria.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.industria.monitor.dto.leitura.LeituraRequestDTO;
import com.industria.monitor.dto.leitura.LeituraResponseDTO;
import com.industria.monitor.dto.maquina.IndicadorProducaoResponseDTO;
import com.industria.monitor.model.Alerta;
import com.industria.monitor.model.Leitura;
import com.industria.monitor.model.Maquina;
import com.industria.monitor.model.Trabalhador;
import com.industria.monitor.model.enums.AcaoAlerta;
import com.industria.monitor.model.enums.EstadoAlerta;
import com.industria.monitor.model.enums.StatusProducao;
import com.industria.monitor.model.enums.TipoRegra;
import com.industria.monitor.repository.AlertaRepository;
import com.industria.monitor.repository.LeituraRepository;
import com.industria.monitor.repository.MaquinaRepository;
import com.industria.monitor.repository.TrabalhadorRepository;
import com.industria.monitor.service.AlertaService;
import com.industria.monitor.service.LeituraService;
import com.industria.monitor.service.MonitoramentoAusenciaService;
import com.industria.monitor.service.MonitoramentoManutencaoService;
import com.industria.monitor.service.MaquinaService;

@SpringBootTest
@Transactional
class MonitorProducaoRegrasTests {

	private static final String CODIGO_ADMIN_TESTE = "ADMIN-TESTE";

	@Autowired
	private MaquinaRepository maquinaRepository;

	@Autowired
	private TrabalhadorRepository trabalhadorRepository;

	@Autowired
	private LeituraRepository leituraRepository;

	@Autowired
	private AlertaRepository alertaRepository;

	@Autowired
	private LeituraService leituraService;

	@Autowired
	private AlertaService alertaService;

	@Autowired
	private MonitoramentoAusenciaService ausenciaService;

	@Autowired
	private MonitoramentoManutencaoService manutencaoService;

	@Autowired
	private MaquinaService maquinaService;

	@BeforeEach
	void criarTrabalhadorAdminBase() {
		trabalhadorRepository.findByCodigo(CODIGO_ADMIN_TESTE).orElseGet(() -> {
			Trabalhador trabalhador = new Trabalhador();
			trabalhador.setCodigo(CODIGO_ADMIN_TESTE);
			trabalhador.setNome("Administrador de teste");
			return trabalhadorRepository.save(trabalhador);
		});
	}

	@Test
	void calculaIndicadoresNaJanelaDeCincoMinutosEUsaOLimiteDaMaquina() {
		LocalDateTime agora = LocalDateTime.now();
		Maquina limiteMaior = criarMaquina("Prensa A", "10");
		Maquina limiteMenor = criarMaquina("Prensa B", "8");
		criarLeitura(limiteMaior, agora.minusMinutes(6), agora.minusMinutes(5), "100");
		criarLeitura(limiteMaior, agora.minusMinutes(4), agora.minusMinutes(3), "9");
		criarLeitura(limiteMenor, agora.minusMinutes(4), agora.minusMinutes(3), "9");

		IndicadorProducaoResponseDTO indicadorA = leituraService.buscarIndicadores(limiteMaior.getId());
		IndicadorProducaoResponseDTO indicadorB = leituraService.buscarIndicadores(limiteMenor.getId());

		assertEquals(0, indicadorA.quantidadeProduzida().compareTo(new BigDecimal("9")));
		assertEquals(StatusProducao.BAIXA, indicadorA.status());
		assertEquals(StatusProducao.NORMAL, indicadorB.status());
	}

	@Test
	void identificaAusenciaSomenteDepoisDeSessentaSegundos() {
		Maquina maquina = criarMaquina("Torno", "1");
		LocalDateTime marco = maquina.getCriadoEm();

		assertFalse(ausenciaService.atualizarEstado(maquina, marco.plusSeconds(60)));
		assertTrue(ausenciaService.atualizarEstado(maquina, marco.plusSeconds(61)));
		assertEquals(1, alertaRepository.findByMaquinaAndTipoRegraAndEstadoNot(
				maquina, TipoRegra.AUSENCIA_DADOS, EstadoAlerta.RESOLVIDO).size());
	}

	@Test
	void reconhecimentoImpedeDuplicacaoAteOAlertaSerResolvido() {
		Maquina maquina = criarMaquina("Esteira", "2");
		Trabalhador trabalhador = trabalhadorAdmin();
		alertaService.abrirSeNecessario(maquina, TipoRegra.LIMITE_MINIMO);
		Alerta alerta = alertaRepository.findAll().get(0);

		alertaService.aplicarAcao(alerta.getId(), AcaoAlerta.RECONHECER, trabalhador.getCodigo(), "");
		alertaService.abrirSeNecessario(maquina, TipoRegra.LIMITE_MINIMO);
		assertEquals(1, alertaRepository.findAll().size());

		alertaService.aplicarAcao(
				alerta.getId(), AcaoAlerta.RESOLVER, trabalhador.getCodigo(), "Produção normalizada");
		alertaService.abrirSeNecessario(maquina, TipoRegra.LIMITE_MINIMO);
		assertEquals(2, alertaRepository.findAll().size());
		assertEquals(EstadoAlerta.RESOLVIDO, alertaRepository.findById(alerta.getId()).orElseThrow().getEstado());
	}

	@Test
	void superproducaoAbreAlertaQuandoUltrapassaLimiteConfigurado() {
		Maquina maquina = criarMaquina("Prensa com limite", "1");
		maquina.setLimiteSuperproducao(new BigDecimal("5"));
		maquinaRepository.save(maquina);
		LocalDateTime agora = LocalDateTime.now();
		criarLeitura(maquina, agora.minusSeconds(30), agora, "10");

		leituraService.buscarIndicadores(maquina.getId());

		assertEquals(1, alertaRepository.findByMaquinaAndTipoRegraAndEstadoNot(
				maquina, TipoRegra.SUPERPRODUCAO, EstadoAlerta.RESOLVIDO).size());
	}

	@Test
	void indicadorNormalNaoResolveAlertaSemAcaoManual() {
		Maquina maquina = criarMaquina("Prensa normalizada", "1");
		Trabalhador trabalhador = trabalhadorAdmin();
		alertaService.abrirSeNecessario(maquina, TipoRegra.LIMITE_MINIMO);
		Alerta alerta = alertaRepository.findAll().get(0);

		LocalDateTime agora = LocalDateTime.now();
		criarLeitura(maquina, agora.minusSeconds(30), agora, "10");
		leituraService.buscarIndicadores(maquina.getId());

		assertEquals(EstadoAlerta.ABERTO, alertaRepository.findById(alerta.getId()).orElseThrow().getEstado());
		alertaService.aplicarAcao(
				alerta.getId(), AcaoAlerta.RESOLVER, trabalhador.getCodigo(), "Verificado pelo monitor");
		assertEquals(EstadoAlerta.RESOLVIDO, alertaRepository.findById(alerta.getId()).orElseThrow().getEstado());
	}

	@Test
	void abreAlertaDeManutencaoNaAntecedenciaConfigurada() {
		Maquina maquina = criarMaquina("Torno com manutenção", "1");
		maquina.setManutencaoProgramadaEm(java.time.LocalDate.now().plusDays(2));
		maquina.setAntecedenciaManutencaoDias(3);
		maquinaRepository.save(maquina);

		manutencaoService.verificarManutencoesProgramadas();

		assertEquals(1, alertaRepository.findByMaquinaAndTipoRegraAndEstadoNot(
				maquina, TipoRegra.MANUTENCAO_PROGRAMADA, EstadoAlerta.RESOLVIDO).size());

		maquina.setManutencaoProgramadaEm(null);
		maquinaRepository.save(maquina);
		manutencaoService.verificarManutencoesProgramadas();
		assertEquals(EstadoAlerta.ABERTO, alertaRepository.findByMaquinaAndTipoRegraAndEstadoNot(
				maquina, TipoRegra.MANUTENCAO_PROGRAMADA, EstadoAlerta.RESOLVIDO).get(0).getEstado());
	}

	@Test
	void edicaoDeMaquinaExigeTrabalhadorCadastradoERegistraResponsavel() {
		Maquina maquina = criarMaquina("Antiga", "2");
		Trabalhador trabalhador = trabalhadorAdmin();
		var atualizada = maquinaService.atualizar(maquina.getId(),
				new com.industria.monitor.dto.maquina.MaquinaRequestDTO(
						"Nova", "un", new BigDecimal("3"), new BigDecimal("30"),
						new BigDecimal("20"), null, 5, trabalhador.getCodigo()));

		assertEquals("Nova", atualizada.nome());
		assertEquals(trabalhador.getCodigo(), atualizada.codigoResponsavelEdicao());
		assertEquals(new BigDecimal("20"), atualizada.limiteSuperproducao());
	}

	@Test
	void edicaoDeLeituraRegistraResponsavelCadastrado() {
		Maquina maquina = criarMaquina("Bomba editável", "2");
		Trabalhador trabalhador = trabalhadorAdmin();
		LocalDateTime inicio = LocalDateTime.now().minusMinutes(1);
		LeituraResponseDTO criada = leituraService.cadastrarLeitura(new LeituraRequestDTO(
				maquina.getId(), inicio, inicio.plusSeconds(30), BigDecimal.ONE, "un"));

		LeituraResponseDTO atualizada = leituraService.atualizar(criada.id(), new LeituraRequestDTO(
				maquina.getId(), inicio, inicio.plusSeconds(30), new BigDecimal("3"),
				"un", criada.chaveEvento(), trabalhador.getCodigo()));

		assertEquals(new BigDecimal("3"), atualizada.quantidade());
		assertEquals(trabalhador.getCodigo(), atualizada.codigoResponsavelEdicao());
	}

	@Test
	void acaoDeManutencaoRegistraResponsavelEObservacoes() {
		Maquina maquina = criarMaquina("Motor", "1");
		Trabalhador trabalhador = trabalhadorAdmin();
		alertaService.abrirSeNecessario(maquina, TipoRegra.MANUTENCAO_PROGRAMADA);
		Alerta alerta = alertaRepository.findAll().get(0);

		var atualizado = alertaService.aplicarAcao(
				alerta.getId(), AcaoAlerta.COLOCAR_EM_MANUTENCAO,
				trabalhador.getCodigo(), "Inspeção iniciada");

		assertEquals(EstadoAlerta.EM_MANUTENCAO, atualizado.estado());
		assertEquals(trabalhador.getCodigo(), atualizado.codigoResponsavel());
		assertEquals("Inspeção iniciada", atualizado.observacoes());
		maquina.setManutencaoProgramadaEm(null);
		maquinaRepository.save(maquina);
		manutencaoService.verificarManutencoesProgramadas();
		assertEquals(EstadoAlerta.EM_MANUTENCAO, alertaRepository.findById(alerta.getId()).orElseThrow().getEstado());
	}

	@Test
	void leituraDuplicadaNaoEInseridaNovamente() {
		Maquina maquina = criarMaquina("Bomba", "1");
		LocalDateTime inicio = LocalDateTime.now().minusMinutes(1);
		LeituraRequestDTO leitura = new LeituraRequestDTO(
				maquina.getId(), inicio, inicio.plusSeconds(30), BigDecimal.ONE, "un");
		leituraService.cadastrarLeitura(leitura);

		Leitura persistida = leituraRepository.findByMaquinaId(maquina.getId()).orElseThrow().get(0);
		assertEquals(inicio, persistida.getInicioIntervalo());
		assertEquals(inicio.plusSeconds(30), persistida.getFimIntervalo());
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> leituraService.cadastrarLeitura(leitura));
		assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
		assertEquals(1, leituraRepository.findByMaquinaId(maquina.getId()).orElse(List.of()).size());
	}

	@Test
	void geraChaveDeEventoParaLeituraManualERejeitaChaveDuplicada() {
		Maquina maquina = criarMaquina("Envasadora", "1");
		LocalDateTime inicio = LocalDateTime.now().minusMinutes(2);
		String chaveEvento = "evento-manual-001";
		LeituraResponseDTO response = leituraService.cadastrarLeitura(new LeituraRequestDTO(
				maquina.getId(), inicio, inicio.plusSeconds(30), BigDecimal.ONE, "un"));

		assertNotNull(response.chaveEvento());
		leituraService.cadastrarLeitura(new LeituraRequestDTO(
				maquina.getId(), inicio.plusMinutes(1), inicio.plusMinutes(1).plusSeconds(30),
				BigDecimal.ONE, "un", chaveEvento));
		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> leituraService.cadastrarLeitura(new LeituraRequestDTO(
						maquina.getId(), inicio.plusMinutes(2), inicio.plusMinutes(2).plusSeconds(30),
						BigDecimal.ONE, "un", chaveEvento)));

		assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
	}

	private Maquina criarMaquina(String nome, String limite) {
		Maquina maquina = new Maquina();
		maquina.setNome(nome);
		maquina.setUnidade("un");
		maquina.setLimiteMin(new BigDecimal(limite));
		maquina.setIntervaloSeg(new BigDecimal("60"));
		return maquinaRepository.save(maquina);
	}

	private Trabalhador trabalhadorAdmin() {
		return trabalhadorRepository.findByCodigo(CODIGO_ADMIN_TESTE)
				.orElseThrow(() -> new IllegalStateException("Trabalhador admin de teste não foi criado"));
	}

	private void criarLeitura(Maquina maquina, LocalDateTime inicio, LocalDateTime fim, String quantidade) {
		Leitura leitura = new Leitura();
		leitura.setMaquina(maquina);
		leitura.setInicioIntervalo(inicio);
		leitura.setFimIntervalo(fim);
		leitura.setQuantidade(new BigDecimal(quantidade));
		leitura.setUnidade(maquina.getUnidade());
		leitura.setChaveEvento(java.util.UUID.randomUUID().toString());
		leituraRepository.save(leitura);
	}
}

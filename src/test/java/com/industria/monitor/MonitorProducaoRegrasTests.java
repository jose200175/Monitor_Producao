package com.industria.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
import com.industria.monitor.model.enums.AcaoAlerta;
import com.industria.monitor.model.enums.EstadoAlerta;
import com.industria.monitor.model.enums.StatusProducao;
import com.industria.monitor.model.enums.TipoRegra;
import com.industria.monitor.repository.AlertaRepository;
import com.industria.monitor.repository.LeituraRepository;
import com.industria.monitor.repository.MaquinaRepository;
import com.industria.monitor.service.AlertaService;
import com.industria.monitor.service.LeituraService;
import com.industria.monitor.service.MonitoramentoAusenciaService;

@SpringBootTest
@Transactional
class MonitorProducaoRegrasTests {

	@Autowired
	private MaquinaRepository maquinaRepository;

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
		alertaService.abrirSeNecessario(maquina, TipoRegra.LIMITE_MINIMO);
		Alerta alerta = alertaRepository.findAll().get(0);

		alertaService.aplicarAcao(alerta.getId(), AcaoAlerta.RECONHECER);
		alertaService.abrirSeNecessario(maquina, TipoRegra.LIMITE_MINIMO);
		assertEquals(1, alertaRepository.findAll().size());

		alertaService.aplicarAcao(alerta.getId(), AcaoAlerta.RESOLVER);
		alertaService.abrirSeNecessario(maquina, TipoRegra.LIMITE_MINIMO);
		assertEquals(2, alertaRepository.findAll().size());
		assertEquals(EstadoAlerta.RESOLVIDO, alertaRepository.findById(alerta.getId()).orElseThrow().getEstado());
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

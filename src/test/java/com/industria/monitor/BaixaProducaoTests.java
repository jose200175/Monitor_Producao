package com.industria.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.industria.monitor.dto.leitura.LeituraRequestDTO;
import com.industria.monitor.dto.maquina.IndicadorProducaoResponseDTO;
import com.industria.monitor.model.Alerta;
import com.industria.monitor.model.Leitura;
import com.industria.monitor.model.Maquina;
import com.industria.monitor.model.enums.EstadoAlerta;
import com.industria.monitor.model.enums.StatusProducao;
import com.industria.monitor.model.enums.TipoRegra;
import com.industria.monitor.repository.AlertaRepository;
import com.industria.monitor.repository.LeituraRepository;
import com.industria.monitor.repository.MaquinaRepository;
import com.industria.monitor.service.LeituraService;

@SpringBootTest
@Transactional
class BaixaProducaoTests {

	@Autowired
	private MaquinaRepository maquinaRepository;

	@Autowired
	private LeituraRepository leituraRepository;

	@Autowired
	private AlertaRepository alertaRepository;

	@Autowired
	private LeituraService leituraService;

	@Test
	void producaoAbaixoDoLimiteFicaBaixaEAbreAlertaDeLimiteMinimo() {
		Maquina maquina = criarMaquina("Prensa abaixo", "10");
		LocalDateTime agora = LocalDateTime.now();
		criarLeitura(maquina, agora.minusSeconds(30), agora, "9");

		IndicadorProducaoResponseDTO indicador = leituraService.buscarIndicadores(maquina.getId());

		assertEquals(StatusProducao.BAIXA, indicador.status());
		assertEquals(1, alertasLimiteMinimo(maquina).size());
		assertEquals(EstadoAlerta.ABERTO, alertasLimiteMinimo(maquina).get(0).getEstado());
	}

	@Test
	void producaoAcimaDoLimiteFicaNormalESemAlerta() {
		Maquina maquina = criarMaquina("Prensa acima", "10");
		LocalDateTime agora = LocalDateTime.now();
		criarLeitura(maquina, agora.minusSeconds(30), agora, "11");

		IndicadorProducaoResponseDTO indicador = leituraService.buscarIndicadores(maquina.getId());

		assertEquals(StatusProducao.NORMAL, indicador.status());
		assertTrue(alertasDaMaquina(maquina).isEmpty());
	}

	@Test
	void producaoIgualAoLimiteFicaNormalESemAlerta() {
		// A regra usa comparação estrita (quantidade < limite), então igual ao limite não é baixa produção.
		Maquina maquina = criarMaquina("Prensa no limite", "10");
		LocalDateTime agora = LocalDateTime.now();
		criarLeitura(maquina, agora.minusSeconds(30), agora, "10");

		IndicadorProducaoResponseDTO indicador = leituraService.buscarIndicadores(maquina.getId());

		assertEquals(0, indicador.quantidadeProduzida().compareTo(new BigDecimal("10")));
		assertEquals(StatusProducao.NORMAL, indicador.status());
		assertTrue(alertasDaMaquina(maquina).isEmpty());
	}

	@Test
	void variasLeiturasBaixasSeguidasAbremApenasUmAlerta() {
		Maquina maquina = criarMaquina("Prensa repetida", "100");
		LocalDateTime inicio = LocalDateTime.now().minusMinutes(3);

		for (int i = 0; i < 4; i++) {
			LocalDateTime inicioLeitura = inicio.plusSeconds(30L * i);
			leituraService.cadastrarLeitura(new LeituraRequestDTO(
					maquina.getId(), inicioLeitura, inicioLeitura.plusSeconds(30), BigDecimal.ONE, "un"));
			leituraService.buscarIndicadores(maquina.getId());
		}

		assertEquals(StatusProducao.BAIXA, leituraService.buscarIndicadores(maquina.getId()).status());
		assertEquals(1, alertasLimiteMinimo(maquina).size());
	}

	private List<Alerta> alertasLimiteMinimo(Maquina maquina) {
		return alertaRepository.findByMaquinaAndTipoRegraAndEstadoNot(
				maquina, TipoRegra.LIMITE_MINIMO, EstadoAlerta.RESOLVIDO);
	}

	private List<Alerta> alertasDaMaquina(Maquina maquina) {
		return alertaRepository.findAll().stream()
				.filter(alerta -> alerta.getMaquina().getId().equals(maquina.getId()))
				.toList();
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

package com.industria.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.industria.monitor.dto.leitura.LeituraResponseDTO;
import com.industria.monitor.dto.maquina.IndicadorProducaoResponseDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;
import com.industria.monitor.exportacao.ExportadorCsv;
import com.industria.monitor.model.enums.StatusProducao;

class ExportadorCsvTests {

	@TempDir
	Path tempDir;

	@Test
	void exportaLeiturasComCamposEscapadosEIdentificacaoDaMaquina() throws Exception {
		MaquinaResponseDTO maquina = new MaquinaResponseDTO(
				7L, "Prensa, \"A\"", "peças/min", BigDecimal.TEN, BigDecimal.valueOf(60));
		LeituraResponseDTO leitura = new LeituraResponseDTO(
				12L, 7L, LocalDateTime.parse("2026-11-17T15:00:00"),
				LocalDateTime.parse("2026-11-17T15:01:00"), new BigDecimal("14.50"),
				"peças/min", "evento,\"001\"", LocalDateTime.parse("2026-11-17T15:01:05"));
		Path arquivo = ExportadorCsv.exportarLeituras(
				tempDir.resolve("exportacoes").resolve("leituras.csv"), maquina, List.of(leitura));

		List<String> linhas = Files.readAllLines(arquivo);
		assertEquals(2, linhas.size());
		assertTrue(linhas.get(0).contains("\"id\",\"maquina_id\",\"maquina\""));
		assertTrue(linhas.get(1).contains("\"Prensa, \"\"A\"\"\""));
		assertTrue(linhas.get(1).contains("\"evento,\"\"001\"\"\""));
		assertTrue(linhas.get(1).contains("\"14.50\""));
	}

	@Test
	void exportaIndicadoresAssociandoNomeDaMaquina() throws Exception {
		MaquinaResponseDTO maquina = new MaquinaResponseDTO(
				3L, "Esteira 3", "un/min", new BigDecimal("8"), BigDecimal.valueOf(60));
		IndicadorProducaoResponseDTO indicador = new IndicadorProducaoResponseDTO(
				3L, LocalDateTime.parse("2026-11-17T15:00:00"),
				LocalDateTime.parse("2026-11-17T15:05:00"), new BigDecimal("6"),
				new BigDecimal("8"), "un/min", StatusProducao.BAIXA);
		Path arquivo = ExportadorCsv.exportarIndicadores(
				tempDir.resolve("indicadores.csv"), List.of(maquina), List.of(indicador));

		List<String> linhas = Files.readAllLines(arquivo);
		assertEquals(2, linhas.size());
		assertTrue(linhas.get(0).contains("\"maquina_id\",\"maquina\",\"inicio_janela\""));
		assertTrue(linhas.get(1).contains("\"3\",\"Esteira 3\""));
		assertTrue(linhas.get(1).contains("\"6\",\"8\",\"un/min\",\"BAIXA\""));
	}

	@Test
	void rejeitaIndicadorSemMaquinaCorrespondente() {
		IndicadorProducaoResponseDTO indicador = new IndicadorProducaoResponseDTO(
				99L, LocalDateTime.now().minusMinutes(5), LocalDateTime.now(),
				BigDecimal.ZERO, BigDecimal.ONE, "un", StatusProducao.SEM_DADOS);

		assertThrows(IllegalArgumentException.class, () -> ExportadorCsv.exportarIndicadores(
				tempDir.resolve("indicadores.csv"), List.of(), List.of(indicador)));
	}

	@Test
	void naoCriaCsvSoComCabecalhoQuandoNaoHaLeituras() {
		Path arquivo = tempDir.resolve("sem-leituras.csv");
		MaquinaResponseDTO maquina = new MaquinaResponseDTO(
				1L, "Prensa", "un", BigDecimal.ONE, BigDecimal.valueOf(60));

		assertThrows(IllegalArgumentException.class,
				() -> ExportadorCsv.exportarLeituras(arquivo, maquina, List.of()));
		assertTrue(Files.notExists(arquivo));
	}
}

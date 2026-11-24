package com.industria.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.industria.monitor.importacao.ImportadorCsv;
import com.industria.monitor.importacao.ResultadoImportacao;
import com.industria.monitor.model.Maquina;

class ImportadorCsvTests {

	@TempDir
	Path tempDir;

	@Test
	void converteLinhasValidasERegistraLinhasInvalidas() throws Exception {
		Maquina maquina = new Maquina();
		maquina.setId(1L);
		Path arquivo = tempDir.resolve("leituras.csv");
		Files.writeString(arquivo,
				"chave_evento,maquina_id,inicio_intervalo,fim_intervalo,quantidade,unidade\n"
						+ "EV-001,1,2026-11-24T10:00:00,2026-11-24T10:01:00,12.5,peças/min\n"
						+ "EV-002,1,2026-11-24T10:01:00,2026-11-24T10:00:00,-2,peças/min\n"
						+ "EV-003,99,2026-11-24T10:02:00,2026-11-24T10:03:00,8,peças/min\n",
				StandardCharsets.UTF_8);

		ResultadoImportacao resultado = ImportadorCsv.importar(arquivo, Map.of(1L, maquina));

		assertEquals(1, resultado.getQuantidadeImportada());
		assertEquals(2, resultado.getQuantidadeRejeitada());
		assertEquals("EV-001", resultado.leiturasImportadas().get(0).getChaveEvento());
		assertTrue(resultado.linhasRejeitadas().get(0).motivo().contains("fim_intervalo"));
	}

	@Test
	void rejeitaArquivoAusenteECabecalhoSemColunasObrigatorias() throws Exception {
		assertThrows(IllegalArgumentException.class,
				() -> ImportadorCsv.importar(tempDir.resolve("ausente.csv"), List.of()));

		Path arquivo = tempDir.resolve("invalido.csv");
		Files.writeString(arquivo, "maquina_id,quantidade\n1,10\n", StandardCharsets.UTF_8);
		IllegalArgumentException excecao = assertThrows(IllegalArgumentException.class,
				() -> ImportadorCsv.importar(arquivo, List.of()));
		assertTrue(excecao.getMessage().contains("inicio_intervalo"));
		assertTrue(excecao.getMessage().contains("fim_intervalo"));
	}
}

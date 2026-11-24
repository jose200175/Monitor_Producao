package com.industria.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.DataOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.industria.monitor.configuracao.ConfiguracaoMaquina;
import com.industria.monitor.configuracao.ConfiguracaoMonitoramento;
import com.industria.monitor.configuracao.PersistenciaConfiguracoesBinarias;

class ConfiguracoesBinariasTests {

	@TempDir
	Path tempDir;

	@Test
	void gravaERecuperaConfiguracoesUsandoStreamsDeDados() throws Exception {
		ConfiguracaoMonitoramento configuracao = new ConfiguracaoMonitoramento(
				300,
				60,
				java.util.List.of(new ConfiguracaoMaquina(
						7L, "Prensa", "peças/min", new BigDecimal("25.50"), new BigDecimal("60.00"))));
		Path arquivo = tempDir.resolve("config").resolve("monitor.dat");

		PersistenciaConfiguracoesBinarias.gravar(arquivo, configuracao);
		ConfiguracaoMonitoramento recuperada =
				PersistenciaConfiguracoesBinarias.recuperar(arquivo);

		assertEquals(configuracao, recuperada);
		assertEquals(new BigDecimal("25.50"),
				recuperada.maquinas().get(0).limiteMinimo());
	}

	@Test
	void rejeitaVersaoIncompativel() throws Exception {
		Path arquivo = tempDir.resolve("versao.dat");
		try (DataOutputStream saida = new DataOutputStream(Files.newOutputStream(arquivo))) {
			saida.writeInt(PersistenciaConfiguracoesBinarias.ASSINATURA_ARQUIVO);
			saida.writeInt(PersistenciaConfiguracoesBinarias.VERSAO_ARQUIVO + 1);
		}

		assertThrows(IllegalArgumentException.class,
				() -> PersistenciaConfiguracoesBinarias.recuperar(arquivo));
	}
}

package com.industria.monitor.configuracao;

import java.io.IOException;
import java.nio.file.Path;

public final class GerenciadorConfiguracoes {

	private GerenciadorConfiguracoes() {
	}

	public static void gravar(Path arquivo, ConfiguracaoMonitoramento configuracao) throws IOException {
		PersistenciaConfiguracoesBinarias.gravar(arquivo, configuracao);
	}

	public static ConfiguracaoMonitoramento recuperar(Path arquivo) throws IOException {
		return PersistenciaConfiguracoesBinarias.recuperar(arquivo);
	}
}

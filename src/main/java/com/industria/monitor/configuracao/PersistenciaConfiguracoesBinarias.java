package com.industria.monitor.configuracao;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class PersistenciaConfiguracoesBinarias {

	public static final int ASSINATURA_ARQUIVO = 0x4D504331;
	public static final int VERSAO_ARQUIVO = ConfiguracaoMonitoramento.VERSAO_ATUAL;

	private PersistenciaConfiguracoesBinarias() {
	}

	public static void gravar(Path arquivo, ConfiguracaoMonitoramento configuracao) throws IOException {
		if (arquivo == null) {
			throw new IllegalArgumentException("Informe o caminho do arquivo de configuração.");
		}
		if (configuracao == null) {
			throw new IllegalArgumentException("Informe as configurações a salvar.");
		}
		Path destino = arquivo.toAbsolutePath().normalize();
		Path diretorio = destino.getParent();
		if (diretorio != null) {
			Files.createDirectories(diretorio);
		}

		try (DataOutputStream saida = new DataOutputStream(Files.newOutputStream(
				destino, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE))) {
			saida.writeInt(ASSINATURA_ARQUIVO);
			saida.writeInt(VERSAO_ARQUIVO);
			saida.writeInt(configuracao.janelaIndicadoresSegundos());
			saida.writeInt(configuracao.limiteAusenciaSegundos());
			saida.writeInt(configuracao.maquinas().size());
			for (ConfiguracaoMaquina maquina : configuracao.maquinas()) {
				saida.writeLong(maquina.maquinaId() == null ? 0L : maquina.maquinaId());
				saida.writeUTF(maquina.nome() == null ? "" : maquina.nome());
				saida.writeUTF(maquina.unidade() == null ? "" : maquina.unidade());
				saida.writeUTF(maquina.limiteMinimo() == null
						? "0" : maquina.limiteMinimo().toPlainString());
				saida.writeUTF(maquina.intervaloSegundos() == null
						? "0" : maquina.intervaloSegundos().toPlainString());
			}
		}
	}

	public static ConfiguracaoMonitoramento recuperar(Path arquivo) throws IOException {
		if (arquivo == null) {
			throw new IllegalArgumentException("Informe o caminho do arquivo de configuração.");
		}
		Path origem = arquivo.toAbsolutePath().normalize();
		if (!Files.exists(origem) || !Files.isRegularFile(origem)) {
			throw new IllegalArgumentException("Arquivo de configuração não encontrado: " + origem);
		}

		try (DataInputStream entrada = new DataInputStream(Files.newInputStream(origem))) {
			int assinatura = entrada.readInt();
			if (assinatura != ASSINATURA_ARQUIVO) {
				throw new IllegalArgumentException("Arquivo de configuração inválido.");
			}
			int versao = entrada.readInt();
			if (versao != VERSAO_ARQUIVO) {
				throw new IllegalArgumentException(
						"Versão de configuração não suportada: " + versao
								+ ". Versão esperada: " + VERSAO_ARQUIVO + ".");
			}
			int janela = entrada.readInt();
			int ausencia = entrada.readInt();
			int quantidade = entrada.readInt();
			if (quantidade < 0) {
				throw new IllegalArgumentException("Quantidade de máquinas inválida no arquivo.");
			}

			List<ConfiguracaoMaquina> maquinas = new ArrayList<>(quantidade);
			for (int i = 0; i < quantidade; i++) {
				long id = entrada.readLong();
				String nome = entrada.readUTF();
				String unidade = entrada.readUTF();
				BigDecimal limite = new BigDecimal(entrada.readUTF());
				BigDecimal intervalo = new BigDecimal(entrada.readUTF());
				maquinas.add(new ConfiguracaoMaquina(
						id == 0L ? null : id, nome, unidade, limite, intervalo));
			}
			return new ConfiguracaoMonitoramento(janela, ausencia, maquinas);
		} catch (java.io.EOFException exception) {
			throw new IllegalArgumentException("Arquivo de configuração incompleto.", exception);
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Arquivo de configuração contém valores numéricos inválidos.",
					exception);
		}
	}
}

package com.industria.monitor.exportacao;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.industria.monitor.dto.leitura.LeituraResponseDTO;
import com.industria.monitor.dto.maquina.IndicadorProducaoResponseDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;

public final class ExportadorCsv {

	private ExportadorCsv() {}

	public static Path exportarLeituras(
			Path arquivo, MaquinaResponseDTO maquina, List<LeituraResponseDTO> leituras) throws IOException {
		exigir(arquivo != null, "Informe o caminho do arquivo CSV.");
		exigir(maquina != null && maquina.id() != null, "Informe uma máquina válida.");
		exigir(leituras != null, "Informe as leituras a exportar.");
		exigir(!leituras.isEmpty(), "Não há leituras para exportar.");

		StringBuilder csv = new StringBuilder();
		adicionarLinha(csv, "id", "maquina_id", "maquina", "chave_evento",
				"inicio_intervalo", "fim_intervalo", "quantidade", "unidade", "criado_em");
		for (LeituraResponseDTO leitura : leituras) {
			exigir(leitura != null && Objects.equals(maquina.id(), leitura.maquinaId()),
					"A lista contém leitura de outra máquina.");
			adicionarLinha(csv,
					texto(leitura.id()),
					texto(leitura.maquinaId()),
					maquina.nome(),
					leitura.chaveEvento(),
					texto(leitura.inicioIntervalo()),
					texto(leitura.fimIntervalo()),
					texto(leitura.quantidade()),
					leitura.unidade(),
					texto(leitura.criadoEm()));
		}
		return gravar(arquivo, csv);
	}

	public static Path exportarIndicadores(
			Path arquivo,
			List<MaquinaResponseDTO> maquinas,
			List<IndicadorProducaoResponseDTO> indicadores) throws IOException {
		exigir(arquivo != null, "Informe o caminho do arquivo CSV.");
		exigir(maquinas != null && indicadores != null, "Informe máquinas e indicadores.");

		Map<Long, MaquinaResponseDTO> maquinasPorId = new HashMap<>();
		for (MaquinaResponseDTO maquina : maquinas) {
			exigir(maquina != null && maquina.id() != null, "A lista contém máquina inválida.");
			exigir(maquinasPorId.putIfAbsent(maquina.id(), maquina) == null,
					"A lista contém máquinas com identificadores duplicados.");
		}
		exigir(indicadores.size() == maquinasPorId.size(),
				"É necessário fornecer um indicador para cada máquina.");

		StringBuilder csv = new StringBuilder();
		adicionarLinha(csv, "maquina_id", "maquina", "inicio_janela", "fim_janela",
				"quantidade_produzida", "limite_minimo", "unidade", "status");
		Set<Long> maquinasExportadas = new HashSet<>();
		for (IndicadorProducaoResponseDTO indicador : indicadores) {
			exigir(indicador != null, "A lista contém indicador inválido.");
			MaquinaResponseDTO maquina = maquinasPorId.get(indicador.maquinaId());
			exigir(maquina != null, "Não há máquina correspondente ao indicador " + indicador.maquinaId() + ".");
			exigir(maquinasExportadas.add(indicador.maquinaId()),
					"Há mais de um indicador para a máquina " + indicador.maquinaId() + ".");
			adicionarLinha(csv,
					texto(indicador.maquinaId()),
					maquina.nome(),
					texto(indicador.inicioJanela()),
					texto(indicador.fimJanela()),
					texto(indicador.quantidadeProduzida()),
					texto(indicador.limiteMinimo()),
					indicador.unidade(),
					texto(indicador.status()));
		}
		exigir(maquinasExportadas.size() == maquinasPorId.size(),
				"Há máquinas sem indicador para exportar.");
		return gravar(arquivo, csv);
	}

	private static Path gravar(Path arquivo, StringBuilder csv) throws IOException {
		Path destino = arquivo.toAbsolutePath().normalize();
		Path diretorio = destino.getParent();
		if (diretorio != null) {
			Files.createDirectories(diretorio);
		}
		Files.writeString(destino, csv, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		return destino;
	}

	private static void adicionarLinha(StringBuilder csv, String... valores) {
		for (int indice = 0; indice < valores.length; indice++) {
			if (indice > 0) {
				csv.append(',');
			}
			String valor = valores[indice] == null ? "" : valores[indice];
			csv.append('"').append(valor.replace("\"", "\"\"")).append('"');
		}
		csv.append("\r\n");
	}

	private static String texto(Object valor) {
		if (valor == null) {
			return "";
		}
		if (valor instanceof BigDecimal decimal) {
			return decimal.toPlainString();
		}
		if (valor instanceof LocalDateTime data) {
			return data.toString();
		}
		return valor.toString();
	}

	private static void exigir(boolean condicao, String mensagem) {
		if (!condicao) {
			throw new IllegalArgumentException(mensagem);
		}
	}
}

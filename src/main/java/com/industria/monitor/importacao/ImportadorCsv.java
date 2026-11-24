package com.industria.monitor.importacao;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.industria.monitor.model.Leitura;
import com.industria.monitor.model.Maquina;

public final class ImportadorCsv {

	private static final List<String> CABECALHO_OBRIGATORIO = List.of(
			"maquina_id", "inicio_intervalo", "fim_intervalo", "quantidade", "unidade");

	private ImportadorCsv() {
	}

	public static ResultadoImportacao importar(java.nio.file.Path arquivo, Collection<Maquina> maquinas)
			throws IOException {
		Map<Long, Maquina> maquinasPorId = new LinkedHashMap<>();
		if (maquinas != null) {
			for (Maquina maquina : maquinas) {
				if (maquina != null && maquina.getId() != null) {
					maquinasPorId.put(maquina.getId(), maquina);
				}
			}
		}
		return importar(arquivo, maquinasPorId);
	}

	public static ResultadoImportacao importar(java.nio.file.Path arquivo, Map<Long, Maquina> maquinasPorId)
			throws IOException {
		if (maquinasPorId == null) {
			throw new IllegalArgumentException("Informe o mapa de máquinas disponíveis.");
		}

		List<List<String>> registros = LeitorCsv.ler(arquivo);
		if (registros.isEmpty()) {
			throw new IllegalArgumentException("Arquivo CSV vazio.");
		}

		Map<String, Integer> indices;
		try {
			indices = LeitorCsv.criarIndices(registros.get(0));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Cabeçalho inválido: " + exception.getMessage(), exception);
		}

		List<String> colunasAusentes = new ArrayList<>();
		for (String coluna : CABECALHO_OBRIGATORIO) {
			if (!indices.containsKey(coluna)) {
				colunasAusentes.add(coluna);
			}
		}
		if (!colunasAusentes.isEmpty()) {
			throw new IllegalArgumentException(
					"Cabeçalho inválido. Colunas ausentes: " + String.join(", ", colunasAusentes));
		}

		List<Leitura> importadas = new ArrayList<>();
		List<LinhaRejeitada> rejeitadas = new ArrayList<>();
		for (int i = 1; i < registros.size(); i++) {
			List<String> campos = registros.get(i);
			String linhaOriginal = String.join(",", campos);
			if (campos.stream().allMatch(String::isBlank)) {
				rejeitadas.add(new LinhaRejeitada(i + 1, linhaOriginal, "Linha vazia."));
				continue;
			}

			try {
				Leitura leitura = converterLinha(campos, indices, maquinasPorId);
				importadas.add(leitura);
			} catch (IllegalArgumentException exception) {
				rejeitadas.add(new LinhaRejeitada(i + 1, linhaOriginal, exception.getMessage()));
			}
		}
		return new ResultadoImportacao(List.copyOf(importadas), List.copyOf(rejeitadas));
	}

	private static Leitura converterLinha(
			List<String> campos,
			Map<String, Integer> indices,
			Map<Long, Maquina> maquinasPorId) {
		int maiorIndice = indices.values().stream().mapToInt(Integer::intValue).max().orElse(-1);
		if (campos.size() <= maiorIndice) {
			throw new IllegalArgumentException("Número de campos menor que o cabeçalho.");
		}

		Long maquinaId = lerLong(obter(campos, indices, "maquina_id"), "máquina_id");
		Maquina maquina = maquinasPorId.get(maquinaId);
		if (maquina == null) {
			throw new IllegalArgumentException("Máquina inexistente: " + maquinaId);
		}

		LocalDateTime inicio = lerDataHora(obter(campos, indices, "inicio_intervalo"), "inicio_intervalo");
		LocalDateTime fim = lerDataHora(obter(campos, indices, "fim_intervalo"), "fim_intervalo");
		if (!inicio.isBefore(fim)) {
			throw new IllegalArgumentException("inicio_intervalo deve ser anterior ao fim_intervalo.");
		}

		BigDecimal quantidade = lerDecimal(obter(campos, indices, "quantidade"), "quantidade");
		if (quantidade.signum() < 0) {
			throw new IllegalArgumentException("quantidade não pode ser negativa.");
		}

		String unidade = lerObrigatorio(campos, indices, "unidade", "unidade");
		String chaveEvento = indices.containsKey("chave_evento")
				? obter(campos, indices, "chave_evento").trim()
				: "";
		if (chaveEvento.length() > 64) {
			throw new IllegalArgumentException("chave_evento excede 64 caracteres.");
		}
		if (chaveEvento.isBlank()) {
			chaveEvento = UUID.randomUUID().toString();
		}

		Leitura leitura = new Leitura();
		leitura.setMaquina(maquina);
		leitura.setInicioIntervalo(inicio);
		leitura.setFimIntervalo(fim);
		leitura.setQuantidade(quantidade);
		leitura.setUnidade(unidade);
		leitura.setChaveEvento(chaveEvento);
		return leitura;
	}

	private static String obter(List<String> campos, Map<String, Integer> indices, String coluna) {
		Integer indice = indices.get(coluna);
		if (indice == null || indice >= campos.size()) {
			return "";
		}
		return campos.get(indice);
	}

	private static String lerObrigatorio(
			List<String> campos, Map<String, Integer> indices, String coluna, String descricao) {
		String valor = obter(campos, indices, coluna).trim();
		if (valor.isBlank()) {
			throw new IllegalArgumentException(descricao + " é obrigatório.");
		}
		return valor;
	}

	private static Long lerLong(String valor, String descricao) {
		try {
			return Long.parseLong(valor.trim());
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException(descricao + " inválido: " + valor, exception);
		}
	}

	private static BigDecimal lerDecimal(String valor, String descricao) {
		try {
			return new BigDecimal(valor.trim().replace(',', '.'));
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException(descricao + " inválida: " + valor, exception);
		}
	}

	private static LocalDateTime lerDataHora(String valor, String descricao) {
		try {
			return LocalDateTime.parse(valor.trim());
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException(descricao + " inválida: " + valor, exception);
		}
	}
}

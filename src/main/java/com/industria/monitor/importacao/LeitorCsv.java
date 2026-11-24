package com.industria.monitor.importacao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LeitorCsv {

	private LeitorCsv() {
	}

	public static Path localizarEVerificar(Path arquivo) throws IOException {
		if (arquivo == null) {
			throw new IllegalArgumentException("Informe o caminho do arquivo CSV.");
		}
		Path absoluto = arquivo.toAbsolutePath().normalize();
		if (!absoluto.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
			throw new IllegalArgumentException("O arquivo deve possuir extensão .csv.");
		}
		if (!Files.exists(absoluto)) {
			throw new IllegalArgumentException("Arquivo CSV não encontrado: " + absoluto);
		}
		if (!Files.isRegularFile(absoluto)) {
			throw new IllegalArgumentException("O caminho informado não é um arquivo CSV.");
		}
		if (!Files.isReadable(absoluto)) {
			throw new IllegalArgumentException("O arquivo CSV não pode ser lido.");
		}
		return absoluto;
	}

	public static List<List<String>> ler(Path arquivo) throws IOException {
		Path verificado = localizarEVerificar(arquivo);
		String texto = Files.readString(verificado, StandardCharsets.UTF_8);
		if (texto.startsWith("\uFEFF")) {
			texto = texto.substring(1);
		}
		return interpretarCsv(texto);
	}

	public static Map<String, Integer> criarIndices(List<String> cabecalho) {
		Map<String, Integer> indices = new LinkedHashMap<>();
		for (int i = 0; i < cabecalho.size(); i++) {
			String nome = cabecalho.get(i).trim().toLowerCase(Locale.ROOT);
			if (nome.isBlank()) {
				throw new IllegalArgumentException("Cabeçalho contém uma coluna sem nome.");
			}
			if (indices.putIfAbsent(nome, i) != null) {
				throw new IllegalArgumentException("Cabeçalho contém a coluna duplicada: " + nome);
			}
		}
		if (indices.isEmpty()) {
			throw new IllegalArgumentException("Cabeçalho do CSV está vazio.");
		}
		return indices;
	}

	static List<List<String>> interpretarCsv(String texto) {
		List<List<String>> registros = new ArrayList<>();
		List<String> registro = new ArrayList<>();
		StringBuilder campo = new StringBuilder();
		boolean entreAspas = false;

		for (int i = 0; i < texto.length(); i++) {
			char atual = texto.charAt(i);
			if (atual == '"') {
				if (entreAspas && i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
					campo.append('"');
					i++;
				} else {
					entreAspas = !entreAspas;
				}
			} else if (atual == ',' && !entreAspas) {
				registro.add(campo.toString());
				campo.setLength(0);
			} else if ((atual == '\n' || atual == '\r') && !entreAspas) {
				if (atual == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
					i++;
				}
				registro.add(campo.toString());
				campo.setLength(0);
				registros.add(registro);
				registro = new ArrayList<>();
			} else {
				campo.append(atual);
			}
		}

		if (entreAspas) {
			throw new IllegalArgumentException("CSV inválido: aspas não fechadas.");
		}
		if (!registro.isEmpty() || campo.length() > 0) {
			registro.add(campo.toString());
			registros.add(registro);
		}
		return registros;
	}
}

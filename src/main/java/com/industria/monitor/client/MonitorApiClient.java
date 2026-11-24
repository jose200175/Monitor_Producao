package com.industria.monitor.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.industria.monitor.dto.alerta.AlertaResponseDTO;
import com.industria.monitor.dto.leitura.LeituraRequestDTO;
import com.industria.monitor.dto.leitura.LeituraResponseDTO;
import com.industria.monitor.dto.maquina.IndicadorProducaoResponseDTO;
import com.industria.monitor.dto.maquina.MaquinaRequestDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;
import com.industria.monitor.model.enums.AcaoAlerta;

public class MonitorApiClient {

	private final URI baseUri;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public MonitorApiClient(String baseUrl) {
		String normalizedUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
		this.baseUri = URI.create(normalizedUrl);
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(3))
				.build();
		this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
	}

	public List<MaquinaResponseDTO> listarMaquinas() throws IOException, InterruptedException {
		return objectMapper.readValue(
				enviar("GET", "maquinas", null),
				new TypeReference<List<MaquinaResponseDTO>>() {});
	}

	public IndicadorProducaoResponseDTO buscarIndicadores(Long maquinaId)
			throws IOException, InterruptedException {
		return objectMapper.readValue(
				enviar("GET", "maquinas/" + maquinaId + "/indicadores", null),
				IndicadorProducaoResponseDTO.class);
	}

	public List<AlertaResponseDTO> listarAlertas() throws IOException, InterruptedException {
		return objectMapper.readValue(
				enviar("GET", "alertas", null),
				new TypeReference<List<AlertaResponseDTO>>() {});
	}

	public List<LeituraResponseDTO> listarLeituras(Long maquinaId) throws IOException, InterruptedException {
		return objectMapper.readValue(
				enviar("GET", "leituras/maquina/" + maquinaId, null),
				new TypeReference<List<LeituraResponseDTO>>() {});
	}

	public MaquinaResponseDTO cadastrarMaquina(MaquinaRequestDTO maquina) throws IOException, InterruptedException {
		return objectMapper.readValue(enviar("POST", "maquinas", maquina), MaquinaResponseDTO.class);
	}

	public MaquinaResponseDTO atualizarMaquina(Long maquinaId, MaquinaRequestDTO maquina)
			throws IOException, InterruptedException {
		return objectMapper.readValue(
				enviar("PUT", "maquinas/" + maquinaId, maquina), MaquinaResponseDTO.class);
	}

	public void cadastrarLeitura(LeituraRequestDTO leitura) throws IOException, InterruptedException {
		enviar("POST", "leituras", leitura);
	}

	public LeituraResponseDTO atualizarLeitura(Long leituraId, LeituraRequestDTO leitura)
			throws IOException, InterruptedException {
		return objectMapper.readValue(
				enviar("PUT", "leituras/" + leituraId, leitura), LeituraResponseDTO.class);
	}

	public void aplicarAcaoAlerta(
			Long alertaId, AcaoAlerta acao, String codigoTrabalhador, String observacoes)
			throws IOException, InterruptedException {
		enviar("PATCH", "alertas/" + alertaId, Map.of(
				"acao", acao.name(),
				"codigoTrabalhador", codigoTrabalhador,
				"observacoes", observacoes == null ? "" : observacoes));
	}

	public int buscarJanelaIndicadoresSegundos() throws IOException, InterruptedException {
		byte[] resposta = enviar("GET", "configuracoes", null);
		Map<?, ?> configuracao = objectMapper.readValue(resposta, Map.class);
		Object segundos = configuracao.get("janelaIndicadoresSegundos");
		return segundos == null ? 300 : Integer.parseInt(segundos.toString());
	}

	public int alterarJanelaIndicadoresSegundos(int segundos) throws IOException, InterruptedException {
		byte[] resposta = enviar("PUT", "configuracoes",
				Map.of("janelaIndicadoresSegundos", segundos));
		Map<?, ?> configuracao = objectMapper.readValue(resposta, Map.class);
		return Integer.parseInt(configuracao.get("janelaIndicadoresSegundos").toString());
	}

	private byte[] enviar(String method, String path, Object body) throws IOException, InterruptedException {
		HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path))
				.timeout(Duration.ofSeconds(10))
				.header("Accept", "application/json");
		if (body == null) {
			request.method(method, HttpRequest.BodyPublishers.noBody());
		} else {
			request.header("Content-Type", "application/json")
					.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
		}

		HttpResponse<byte[]> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			String responseBody = mensagemErro(response.body());
			throw new IOException("API respondeu HTTP " + response.statusCode()
					+ (responseBody.isBlank() ? "" : ": " + responseBody));
		}
		return response.body();
	}

	private String mensagemErro(byte[] corpo) {
		String texto = new String(corpo, java.nio.charset.StandardCharsets.UTF_8);
		try {
			JsonNode erro = objectMapper.readTree(texto);
			for (String campo : List.of("message", "detail", "error")) {
				JsonNode valor = erro.get(campo);
				if (valor != null && valor.isTextual() && !valor.asText().isBlank()) {
					return valor.asText();
				}
			}
		} catch (JsonProcessingException ignored) {
			return texto;
		}
		return texto;
	}
}

package com.industria.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import com.industria.monitor.client.MonitorApiClient;
import com.industria.monitor.controller.ApiExceptionHandler;
import com.industria.monitor.dto.configuracao.ConfiguracaoMonitoramentoDTO;
import com.industria.monitor.dto.maquina.MaquinaRequestDTO;
import com.industria.monitor.dto.maquina.MaquinaResponseDTO;
import com.industria.monitor.model.enums.AcaoAlerta;
import com.industria.monitor.repository.MaquinaRepository;
import com.industria.monitor.service.LeituraService;
import com.industria.monitor.service.MaquinaService;
import com.sun.net.httpserver.HttpServer;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

@SpringBootTest
@Transactional
class TratamentoNulosTests {

	@Autowired
	private MaquinaService maquinaService;

	@Autowired
	private LeituraService leituraService;

	@Autowired
	private MaquinaRepository maquinaRepository;

	@Test
	void maquinaSemIntervaloUsaSessentaSegundosComoPadrao() {
		MaquinaResponseDTO criada = maquinaService.criarMaquina(
				new MaquinaRequestDTO("Prensa sem intervalo", "un", new BigDecimal("5"), null));

		BigDecimal intervalo = maquinaRepository.findById(criada.id()).orElseThrow().getIntervaloSeg();
		assertEquals(0, intervalo.compareTo(new BigDecimal("60")));
	}

	@Test
	void maquinaNulaRetornaBadRequest() {
		ResponseStatusException criar = assertThrows(ResponseStatusException.class,
				() -> maquinaService.criarMaquina(null));
		ResponseStatusException atualizar = assertThrows(ResponseStatusException.class,
				() -> maquinaService.atualizar(1L, null));

		assertEquals(HttpStatus.BAD_REQUEST, criar.getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, atualizar.getStatusCode());
	}

	@Test
	void leituraNulaRetornaBadRequest() {
		ResponseStatusException cadastrar = assertThrows(ResponseStatusException.class,
				() -> leituraService.cadastrarLeitura(null));
		ResponseStatusException atualizar = assertThrows(ResponseStatusException.class,
				() -> leituraService.atualizar(1L, null));

		assertEquals(HttpStatus.BAD_REQUEST, cadastrar.getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, atualizar.getStatusCode());
	}

	@Test
	void janelaDeIndicadoresNulaEhRejeitadaNaValidacao() {
		Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

		assertFalse(validator.validate(new ConfiguracaoMonitoramentoDTO(null)).isEmpty());
		assertTrue(validator.validate(new ConfiguracaoMonitoramentoDTO(60)).isEmpty());
	}

	@Test
	void corpoAusenteOuInvalidoRetornaBadRequest() {
		var response = new ApiExceptionHandler().handleCorpoInvalido(new HttpMessageNotReadableException(
				"JSON inválido", new MockHttpInputMessage(new byte[0])));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("O corpo da requisição está ausente ou é inválido.", response.getBody().message());
	}

	@Test
	void idNaoNumericoNaUrlRetornaBadRequest() throws NoSuchMethodException {
		MethodParameter parametro = new MethodParameter(
				TratamentoNulosTests.class.getDeclaredMethod("endpointFicticio", Long.class), 0);
		var response = new ApiExceptionHandler().handleTipoInvalido(new MethodArgumentTypeMismatchException(
				"abc", Long.class, "maquinaId", parametro, new NumberFormatException()));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Valor inválido para o parâmetro 'maquinaId'.", response.getBody().message());
	}

	@Test
	void illegalArgumentRetornaBadRequestComMensagem() {
		ApiExceptionHandler handler = new ApiExceptionHandler();

		var comMensagem = handler.handleArgumentoInvalido(
				new IllegalArgumentException("A janela de indicadores deve ser maior que zero."));
		var semMensagem = handler.handleArgumentoInvalido(new IllegalArgumentException());

		assertEquals(HttpStatus.BAD_REQUEST, comMensagem.getStatusCode());
		assertEquals("A janela de indicadores deve ser maior que zero.", comMensagem.getBody().message());
		assertEquals("Os dados informados são inválidos.", semMensagem.getBody().message());
	}

	@Test
	void clienteAceitaCodigoTrabalhadorNuloEJanelaAusente() throws Exception {
		AtomicReference<String> corpoRecebido = new AtomicReference<>();
		HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		servidor.createContext("/", exchange -> {
			corpoRecebido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] resposta = "{}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, resposta.length);
			exchange.getResponseBody().write(resposta);
			exchange.close();
		});
		servidor.start();
		try {
			MonitorApiClient client = new MonitorApiClient(
					"http://localhost:" + servidor.getAddress().getPort());

			client.aplicarAcaoAlerta(1L, AcaoAlerta.RECONHECER, null, null);
			assertTrue(corpoRecebido.get().contains("\"codigoTrabalhador\":null"));

			assertEquals(300, client.buscarJanelaIndicadoresSegundos());
			assertEquals(300, client.alterarJanelaIndicadoresSegundos(120));
		} finally {
			servidor.stop(0);
		}
	}

	@SuppressWarnings("unused")
	private void endpointFicticio(Long maquinaId) {
	}
}

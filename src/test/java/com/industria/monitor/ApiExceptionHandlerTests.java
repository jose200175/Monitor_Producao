package com.industria.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.industria.monitor.controller.ApiExceptionHandler;

class ApiExceptionHandlerTests {

	@Test
	void incluiMotivoDaFalhaNaRespostaDaApi() {
		var response = new ApiExceptionHandler().handleResponseStatus(
				new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de trabalhador não cadastrado"));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Código de trabalhador não cadastrado", response.getBody().message());
	}
}

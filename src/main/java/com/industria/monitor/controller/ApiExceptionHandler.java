package com.industria.monitor.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
		String message = exception.getReason() == null
				? exception.getStatusCode().toString()
				: exception.getReason();
		return ResponseEntity.status(exception.getStatusCode()).body(new ApiError(message));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getDefaultMessage())
				.distinct()
				.collect(Collectors.joining("; "));
		if (message.isBlank()) {
			message = "Os dados informados são inválidos.";
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message));
	}

	public record ApiError(String message) {
	}
}

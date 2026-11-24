package com.industria.monitor.configuracao;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

@Component
public class ConfiguracaoMonitoramentoRuntime {

	private final AtomicReference<Duration> janelaIndicadores =
			new AtomicReference<>(Duration.ofMinutes(5));

	public Duration getJanelaIndicadores() {
		return janelaIndicadores.get();
	}

	public int getJanelaIndicadoresSegundos() {
		return (int) janelaIndicadores.get().getSeconds();
	}

	public synchronized void setJanelaIndicadoresSegundos(int segundos) {
		if (segundos <= 0) {
			throw new IllegalArgumentException("A janela de indicadores deve ser maior que zero.");
		}
		janelaIndicadores.set(Duration.ofSeconds(segundos));
	}
}

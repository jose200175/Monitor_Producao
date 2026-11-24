package com.industria.monitor.simulacao;

import java.util.Map;
import java.util.function.Consumer;

import com.industria.monitor.dto.maquina.MaquinaResponseDTO;

public interface FonteLeitura extends AutoCloseable {

	void iniciar(
			Map<CenarioSimulacao, MaquinaResponseDTO> destino,
			Consumer<String> aoEnviar,
			Consumer<Exception> aoFalhar);

	void parar();

	boolean isExecutando();

	@Override
	void close();
}

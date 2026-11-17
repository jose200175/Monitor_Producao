package com.industria.monitor.simulacao;

public enum CenarioSimulacao {
	PRODUCAO_NORMAL("Simulador normal", "Produção acima do limite mínimo"),
	BAIXA_PRODUCAO("Simulador baixa produção", "Produção abaixo do limite mínimo"),
	AUSENCIA_ENVIO("Simulador sem envio", "Máquina cadastrada sem novas leituras");

	private final String nomeMaquina;
	private final String descricao;

	CenarioSimulacao(String nomeMaquina, String descricao) {
		this.nomeMaquina = nomeMaquina;
		this.descricao = descricao;
	}

	public String nomeMaquina() {
		return nomeMaquina;
	}

	public String descricao() {
		return descricao;
	}
}

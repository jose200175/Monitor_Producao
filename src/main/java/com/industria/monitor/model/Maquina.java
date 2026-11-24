package com.industria.monitor.model;


import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name= "maquinas")
public class Maquina implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@Column(name = "nome", nullable = false)
    private String nome;
	
	@Column(name = "unidade", nullable = false)
    private String unidade;
	
	@Column(name = "limite_min", nullable = false)
    private BigDecimal limiteMin;
	
	@Column(name = "intervalo_seg", nullable = false, columnDefinition = "DECIMAL(10,2) DEFAULT 60.00")
    private BigDecimal intervaloSeg;

	@Column(name = "limite_superproducao")
	private BigDecimal limiteSuperproducao;

	@Column(name = "manutencao_programada_em")
	private LocalDate manutencaoProgramadaEm;

	@Column(name = "antecedencia_manutencao_dias", nullable = false)
	private Integer antecedenciaManutencaoDias = 7;

	@ManyToOne
	@JoinColumn(name = "responsavel_edicao_id")
	private Trabalhador responsavelEdicao;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private LocalDateTime criadoEm;
	
	@OneToMany(mappedBy = "maquina", cascade = CascadeType.ALL)
	private List<Leitura> leituras;
	
	@OneToMany(mappedBy = "maquina", cascade = CascadeType.ALL)
	private List<Alerta> alertas;

	@PrePersist
	protected void onCreate() {
		this.criadoEm = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getUnidade() {
		return unidade;
	}

	public void setUnidade(String unidade) {
		this.unidade = unidade;
	}

	public BigDecimal getLimiteMin() {
		return limiteMin;
	}

	public void setLimiteMin(BigDecimal limiteMin) {
		this.limiteMin = limiteMin;
	}

	public BigDecimal getIntervaloSeg() {
		return intervaloSeg;
	}

	public void setIntervaloSeg(BigDecimal intervaloSeg) {
		this.intervaloSeg = intervaloSeg;
	}

	public BigDecimal getLimiteSuperproducao() {
		return limiteSuperproducao;
	}

	public void setLimiteSuperproducao(BigDecimal limiteSuperproducao) {
		this.limiteSuperproducao = limiteSuperproducao;
	}

	public LocalDate getManutencaoProgramadaEm() {
		return manutencaoProgramadaEm;
	}

	public void setManutencaoProgramadaEm(LocalDate manutencaoProgramadaEm) {
		this.manutencaoProgramadaEm = manutencaoProgramadaEm;
	}

	public Integer getAntecedenciaManutencaoDias() {
		return antecedenciaManutencaoDias;
	}

	public void setAntecedenciaManutencaoDias(Integer antecedenciaManutencaoDias) {
		this.antecedenciaManutencaoDias = antecedenciaManutencaoDias;
	}

	public Trabalhador getResponsavelEdicao() {
		return responsavelEdicao;
	}

	public void setResponsavelEdicao(Trabalhador responsavelEdicao) {
		this.responsavelEdicao = responsavelEdicao;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}

	public void setCriadoEm(LocalDateTime criadoEm) {
		this.criadoEm = criadoEm;
	}

	public List<Leitura> getLeituras() {
		return leituras;
	}

	public void setLeituras(List<Leitura> leituras) {
		this.leituras = leituras;
	}

	public List<Alerta> getAlertas() {
		return alertas;
	}

	public void setAlertas(List<Alerta> alertas) {
		this.alertas = alertas;
	}
}

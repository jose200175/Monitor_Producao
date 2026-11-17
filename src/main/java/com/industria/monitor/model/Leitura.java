package com.industria.monitor.model;


import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name= "leituras", uniqueConstraints = {@UniqueConstraint(columnNames={"maquina_id", "inicio_intervalo", "fim_intervalo"})})
public class Leitura implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne
	@JoinColumn( name = "maquina_id")
	private Maquina maquina;
	
	@Column(name = "inicio_intervalo", nullable = false)
    private LocalDateTime inicioIntervalo;

	@Column(name = "fim_intervalo", nullable = false)
    private LocalDateTime fimIntervalo;
	
	@Column(name = "quantidade", nullable = false)
	private BigDecimal quantidade;
	
	@Column(name = "unidade", nullable = false)
	private String unidade;

	@Column(name = "chave_evento", nullable = false, unique = true, length = 64)
	private String chaveEvento;
	
	@CreatedDate
	@Column(name="criado_em", nullable= false, updatable = false)
	private LocalDateTime criadoEm;
	
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

	public Maquina getMaquina() {
		return maquina;
	}

	public void setMaquina(Maquina maquina) {
		this.maquina = maquina;
	}

	public LocalDateTime getInicioIntervalo() {
		return inicioIntervalo;
	}

	public void setInicioIntervalo(LocalDateTime inicioIntervalo) {
		this.inicioIntervalo = inicioIntervalo;
	}

	public LocalDateTime getFimIntervalo() {
		return fimIntervalo;
	}

	public void setFimIntervalo(LocalDateTime fimIntervalo) {
		this.fimIntervalo = fimIntervalo;
	}

	public BigDecimal getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(BigDecimal quantidade) {
		this.quantidade = quantidade;
	}

	public String getUnidade() {
		return unidade;
	}

	public void setUnidade(String unidade) {
		this.unidade = unidade;
	}

	public String getChaveEvento() {
		return chaveEvento;
	}

	public void setChaveEvento(String chaveEvento) {
		this.chaveEvento = chaveEvento;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}

	public void setCriadoEm(LocalDateTime criadoEm) {
		this.criadoEm = criadoEm;
	}
}

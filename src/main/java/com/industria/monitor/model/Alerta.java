package com.industria.monitor.model;


import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.industria.monitor.model.enums.EstadoAlerta;
import com.industria.monitor.model.enums.TipoRegra;

@Entity
@Table(name= "alertas")
public class Alerta implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public Alerta() {
	}

	public Alerta(Long id, Maquina maquina, TipoRegra tipoRegra, EstadoAlerta estado, LocalDateTime criadoEm) {
		this.id = id;
		this.maquina = maquina;
		this.tipoRegra = tipoRegra;
		this.estado = estado;
		this.criadoEm = criadoEm;
	}

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne
	@JoinColumn( name = "maquina_id", nullable = false)
	private Maquina maquina;
	
	@Enumerated(EnumType.STRING)
	@Column(name="tipo_regra", nullable = false)
	private TipoRegra tipoRegra;
	
	@Enumerated(EnumType.STRING)
	@Column(name ="estado_alerta", nullable = false)
	private EstadoAlerta estado;
	
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

	public TipoRegra getTipoRegra() {
		return tipoRegra;
	}

	public void setTipoRegra(TipoRegra tipoRegra) {
		this.tipoRegra = tipoRegra;
	}

	public EstadoAlerta getEstado() {
		return estado;
	}

	public void setEstado(EstadoAlerta estado) {
		this.estado = estado;
	}

	public LocalDateTime getCriadoEm() {
		return criadoEm;
	}

	public void setCriadoEm(LocalDateTime criadoEm) {
		this.criadoEm = criadoEm;
	}
}


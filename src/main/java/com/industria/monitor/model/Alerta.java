package com.industria.monitor.model;


import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.industria.monitor.model.enums.EstadoAlerta;
import com.industria.monitor.model.enums.TipoRegra;

@Entity
@Table(name= "alertas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerta implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
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
}


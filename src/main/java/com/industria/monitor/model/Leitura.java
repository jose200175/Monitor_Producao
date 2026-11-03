package com.industria.monitor.model;


import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name= "leituras", uniqueConstraints = {@UniqueConstraint(columnNames={"maquina_id", "inicio_intervalo", "fim_intervalo"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leitura implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne
	@JoinColumn( name = "maquina_id")
	private Maquina maquina;
	
	@Column(name = "inicio_inrervalo", nullable = false)
    private LocalDateTime inicioIntervalo;

	@Column(name = "fim_inrervalo", nullable = false)
    private LocalDateTime fimIntervalo;
	
	@Column(name = "quantidade", nullable = false)
	private BigDecimal quantidade;
	
	@Column(name = "unidade", nullable = false)
	private String unidade;
	
	@CreatedDate
	@Column(name="criado_em", nullable= false, updatable = false)
	private LocalDateTime criadoEm;
	
	@PrePersist
	protected void onCreate() {
	    this.criadoEm = LocalDateTime.now();
	}
}

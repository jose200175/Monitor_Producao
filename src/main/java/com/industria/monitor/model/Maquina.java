package com.industria.monitor.model;


import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name= "maquinas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
	
	@OneToMany(mappedBy = "maquina", cascade = CascadeType.ALL)
	private List<Leitura> leituras;
	
	@OneToMany(mappedBy = "maquina", cascade = CascadeType.ALL)
	private List<Alerta> alertas;
}

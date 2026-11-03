package com.industria.monitor.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.industria.monitor.model.Leitura;

public interface LeituraRepository extends JpaRepository<Leitura, Long>{

	public boolean existsByMaquinaIdAndInicioIntervaloAndFimIntervalo(Long maquinaId, LocalDateTime inicioIntervalo, LocalDateTime fimIntervalo);
	public Optional<List<Leitura>> findByMaquinaId(Long maquinaId);
}

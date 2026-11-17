package com.industria.monitor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.industria.monitor.model.Alerta;
import com.industria.monitor.model.Maquina;
import com.industria.monitor.model.enums.EstadoAlerta;
import com.industria.monitor.model.enums.TipoRegra;

public interface AlertaRepository extends JpaRepository<Alerta, Long>{
	Optional<Alerta> findByMaquinaAndEstadoNot(Maquina maquina, EstadoAlerta estado); //função usada para buscar alertas que não esteja resolvidos
	List<Alerta> findByMaquinaAndTipoRegraAndEstadoNot(Maquina maquina, TipoRegra tipoRegra, EstadoAlerta estado);
}

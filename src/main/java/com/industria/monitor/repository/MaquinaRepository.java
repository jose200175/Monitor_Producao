package com.industria.monitor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.industria.monitor.model.Maquina;

public interface MaquinaRepository extends JpaRepository<Maquina, Long>{
	
}

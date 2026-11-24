package com.industria.monitor.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.industria.monitor.model.Trabalhador;

public interface TrabalhadorRepository extends JpaRepository<Trabalhador, Long> {
	Optional<Trabalhador> findByCodigo(String codigo);
}

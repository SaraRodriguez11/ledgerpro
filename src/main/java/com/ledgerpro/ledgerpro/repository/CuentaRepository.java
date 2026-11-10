package com.ledgerpro.ledgerpro.repository;

import com.ledgerpro.ledgerpro.domain.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    // Spring deduce la consulta SQL desde el nombre del método
    Optional<Cuenta> findByCodigo(String codigo);
}
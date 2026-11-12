package com.ledgerpro.ledgerpro.service;

import com.ledgerpro.ledgerpro.DTOs.reporte.BalanceComprobacion;
import com.ledgerpro.ledgerpro.DTOs.reporte.EstadoResultados;
import com.ledgerpro.ledgerpro.DTOs.reporte.LibroMayor;

import java.time.LocalDate;

public interface ReporteService {

    BalanceComprobacion balanceComprobacion(LocalDate desde, LocalDate hasta);

    EstadoResultados estadoResultados(LocalDate desde, LocalDate hasta);

    LibroMayor libroMayor(String codigoCuenta, LocalDate desde, LocalDate hasta);
}
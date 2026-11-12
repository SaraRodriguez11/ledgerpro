package com.ledgerpro.ledgerpro.DTOs.reporte;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EstadoResultados(
        LocalDate desde,
        LocalDate hasta,
        List<LineaBalance> ingresos,
        List<LineaBalance> gastos,
        BigDecimal totalIngresos,
        BigDecimal totalGastos,
        BigDecimal utilidadNeta
) {}

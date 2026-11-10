package com.ledgerpro.ledgerpro.DTOs;

import com.ledgerpro.ledgerpro.domain.Asiento;
import com.ledgerpro.ledgerpro.domain.EstadoAsiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AsientoResponse(
        Long id,
        LocalDate fecha,
        String descripcion,
        EstadoAsiento estado,
        BigDecimal totalDebitos,
        BigDecimal totalCreditos,
        List<LineaAsientoResponse> lineas
) {
    public static AsientoResponse desde(Asiento asiento) {
        return new AsientoResponse(
                asiento.getId(),
                asiento.getFecha(),
                asiento.getDescripcion(),
                asiento.getEstado(),
                asiento.totalDebitos(),
                asiento.totalCreditos(),
                asiento.getLineas().stream().map(LineaAsientoResponse::desde).toList());
    }
}
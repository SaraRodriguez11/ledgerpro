package com.ledgerpro.ledgerpro.DTOs;

import com.ledgerpro.ledgerpro.domain.LineaAsiento;

import java.math.BigDecimal;

public record LineaAsientoResponse(
        String codigoCuenta,
        String nombreCuenta,
        BigDecimal debito,
        BigDecimal credito
) {
    public static LineaAsientoResponse desde(LineaAsiento linea) {
        return new LineaAsientoResponse(
                linea.getCuenta().getCodigo(),
                linea.getCuenta().getNombre(),
                linea.getDebito(),
                linea.getCredito());
    }
}
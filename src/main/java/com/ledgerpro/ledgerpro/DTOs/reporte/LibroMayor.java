package com.ledgerpro.ledgerpro.DTOs.reporte;

import com.ledgerpro.ledgerpro.domain.TipoCuenta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LibroMayor(
        String codigoCuenta,
        String nombreCuenta,
        TipoCuenta tipo,
        LocalDate desde,
        LocalDate hasta,
        BigDecimal saldoInicial,
        List<Movimiento> movimientos,
        BigDecimal saldoFinal
) {
    public record Movimiento(
            LocalDate fecha,
            Long asientoId,
            String descripcion,
            BigDecimal debito,
            BigDecimal credito,
            BigDecimal saldo      // saldo acumulado después de este movimiento
    ) {}
}

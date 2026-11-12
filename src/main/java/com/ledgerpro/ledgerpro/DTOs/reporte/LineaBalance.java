package com.ledgerpro.ledgerpro.DTOs.reporte;

import com.ledgerpro.ledgerpro.domain.TipoCuenta;
import java.math.BigDecimal;

public record LineaBalance(
        String codigo,
        String nombre,
        TipoCuenta tipo,
        BigDecimal totalDebitos,
        BigDecimal totalCreditos,
        BigDecimal saldo
) {}
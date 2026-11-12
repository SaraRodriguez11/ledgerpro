package com.ledgerpro.ledgerpro.DTOs.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BalanceComprobacion(
        LocalDate desde,
        LocalDate hasta,
        List<LineaBalance> cuentas,
        BigDecimal totalDebitos,
        BigDecimal totalCreditos,
        boolean cuadrado          // control: los totales deben ser iguales
) {}


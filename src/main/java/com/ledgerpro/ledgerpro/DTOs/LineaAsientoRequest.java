package com.ledgerpro.ledgerpro.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record LineaAsientoRequest(
        @NotBlank String codigoCuenta,
        @NotNull @PositiveOrZero BigDecimal debito,
        @NotNull @PositiveOrZero BigDecimal credito
) {}
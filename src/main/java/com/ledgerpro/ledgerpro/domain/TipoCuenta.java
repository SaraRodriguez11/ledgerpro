package com.ledgerpro.ledgerpro.domain;

import java.math.BigDecimal;

/** Clasificación contable de una cuenta y su naturaleza (deudora o acreedora). */
public enum TipoCuenta {
    ACTIVO(true),
    PASIVO(false),
    PATRIMONIO(false),
    INGRESO(false),
    GASTO(true);

    private final boolean deudora;

    TipoCuenta(boolean deudora) {
        this.deudora = deudora;
    }

    public boolean esDeudora() {
        return deudora;
    }

    /** Saldo según la naturaleza: deudoras suman débitos, acreedoras suman créditos. */
    public BigDecimal calcularSaldo(BigDecimal debitos, BigDecimal creditos) {
        return deudora ? debitos.subtract(creditos) : creditos.subtract(debitos);
    }
}
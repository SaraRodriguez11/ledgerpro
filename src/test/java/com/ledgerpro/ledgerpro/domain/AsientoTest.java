package com.ledgerpro.ledgerpro.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AsientoTest {

    private final Cuenta caja = new Cuenta("1105", "Caja", TipoCuenta.ACTIVO, null);
    private final Cuenta ventas = new Cuenta("4135", "Ventas", TipoCuenta.INGRESO, null);

    @Test
    void estaBalanceado_cuandoDebitosIgualanCreditos() {
        Asiento asiento = new Asiento(LocalDate.of(2026, 11, 10), "Venta");
        asiento.agregarLinea(caja, new BigDecimal("100.00"), BigDecimal.ZERO);
        asiento.agregarLinea(ventas, BigDecimal.ZERO, new BigDecimal("100.00"));

        assertThat(asiento.estaBalanceado()).isTrue();
    }

    @Test
    void estaBalanceado_ignoraLaEscalaDelDecimal() {
        // 100.0 y 100.00 son iguales en valor aunque BigDecimal.equals diga que no
        Asiento asiento = new Asiento(LocalDate.of(2026, 11, 10), "Venta");
        asiento.agregarLinea(caja, new BigDecimal("100.0"), BigDecimal.ZERO);
        asiento.agregarLinea(ventas, BigDecimal.ZERO, new BigDecimal("100.00"));

        assertThat(asiento.estaBalanceado()).isTrue();
    }

    @Test
    void noEstaBalanceado_cuandoLosTotalesDifieren() {
        Asiento asiento = new Asiento(LocalDate.of(2026, 11, 10), "Venta");
        asiento.agregarLinea(caja, new BigDecimal("100.00"), BigDecimal.ZERO);
        asiento.agregarLinea(ventas, BigDecimal.ZERO, new BigDecimal("90.00"));

        assertThat(asiento.estaBalanceado()).isFalse();
    }
}
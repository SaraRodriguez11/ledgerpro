package com.ledgerpro.ledgerpro.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Una línea de un asiento: un débito o un crédito contra una cuenta. */
@Entity
@Table(name = "linea_asiento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LineaAsiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asiento_id", nullable = false)
    private Asiento asiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal debito = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal credito = BigDecimal.ZERO;

    // Visibilidad de paquete: solo Asiento puede crear sus líneas
    LineaAsiento(Asiento asiento, Cuenta cuenta, BigDecimal debito, BigDecimal credito) {
        this.asiento = asiento;
        this.cuenta = cuenta;
        this.debito = debito;
        this.credito = credito;
    }
}
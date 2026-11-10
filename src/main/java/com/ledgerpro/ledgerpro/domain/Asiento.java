package com.ledgerpro.ledgerpro.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Encabezado de un movimiento contable. Agrupa sus líneas de débito y crédito. */
@Entity
@Table(name = "asiento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoAsiento estado = EstadoAsiento.CONTABILIZADO;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    // cascade ALL: al guardar el asiento se guardan sus líneas (misma transacción)
    // orphanRemoval: una línea quitada de la lista se elimina de la base
    @OneToMany(mappedBy = "asiento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE) // no exponemos la lista modificable
    private List<LineaAsiento> lineas = new ArrayList<>();

    public Asiento(LocalDate fecha, String descripcion) {
        this.fecha = fecha;
        this.descripcion = descripcion;
    }

    @PrePersist
    void alCrear() {
        this.creadoEn = Instant.now();
    }

    /** Agrega una línea. La única vía para hacerlo, así la relación siempre queda coherente. */
    public void agregarLinea(Cuenta cuenta, BigDecimal debito, BigDecimal credito) {
        lineas.add(new LineaAsiento(this, cuenta, debito, credito));
    }

    /** Vista de solo lectura: nadie de afuera puede alterar la lista directamente. */
    public List<LineaAsiento> getLineas() {
        return Collections.unmodifiableList(lineas);
    }

    public BigDecimal totalDebitos() {
        return lineas.stream().map(LineaAsiento::getDebito)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalCreditos() {
        return lineas.stream().map(LineaAsiento::getCredito)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Regla de partida doble: débitos y créditos deben ser iguales. */
    public boolean estaBalanceado() {
        // compareTo y no equals: equals considera distinta la escala (2.0 vs 2.00)
        return totalDebitos().compareTo(totalCreditos()) == 0;
    }

    public void anular() {
        this.estado = EstadoAsiento.ANULADO;
    }
}
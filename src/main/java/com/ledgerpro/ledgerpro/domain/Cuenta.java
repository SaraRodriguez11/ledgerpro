package com.ledgerpro.ledgerpro.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Una cuenta del plan de cuentas (por ejemplo, 1105 - Caja). */
@Entity
@Table(name = "cuenta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA lo exige; protected evita su uso accidental
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // la base genera el id
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING) // guarda el texto "ACTIVO", no el número de posición
    @Column(nullable = false, length = 15)
    private TipoCuenta tipo;

    @ManyToOne(fetch = FetchType.LAZY) // solo carga la cuenta padre cuando se necesita
    @JoinColumn(name = "cuenta_padre_id")
    private Cuenta cuentaPadre;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    public Cuenta(String codigo, String nombre, TipoCuenta tipo, Cuenta cuentaPadre) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.cuentaPadre = cuentaPadre;
    }

    @PrePersist // se ejecuta justo antes del INSERT
    void alCrear() {
        this.creadaEn = Instant.now();
    }

    public void desactivar() {
        this.activa = false;
    }
}
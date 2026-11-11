package com.ledgerpro.ledgerpro.service;

import com.ledgerpro.ledgerpro.DTOs.AsientoRequest;
import com.ledgerpro.ledgerpro.DTOs.AsientoResponse;
import com.ledgerpro.ledgerpro.DTOs.LineaAsientoRequest;
import com.ledgerpro.ledgerpro.exception.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración contra PostgreSQL real (base ledgerpro_test).
 * OJO: la clase NO es @Transactional a propósito. Si lo fuera, el servicio se uniría a la
 * transacción de la prueba y no podríamos observar el commit ni el rollback reales.
 */
@SpringBootTest
@ActiveProfiles("test")
class AsientoServiceIntegracionTest {

    @Autowired private AsientoService servicio;
    @Autowired private JdbcTemplate jdbc;
    @Value("${spring.datasource.url}") private String urlBaseDeDatos;

    @BeforeEach
    void limpiar() {
        // Seguro de vida: jamás borrar datos si por error apuntamos a la base de desarrollo
        assertThat(urlBaseDeDatos).endsWith("_test");
        jdbc.execute("DELETE FROM linea_asiento");
        jdbc.execute("DELETE FROM asiento");
    }

    private long contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }

    private AsientoRequest ventaDeContado(String creditoVentas) {
        return new AsientoRequest(LocalDate.of(2026, 11, 10), "Venta de contado", List.of(
                new LineaAsientoRequest("1105", new BigDecimal("500000.00"), BigDecimal.ZERO),
                new LineaAsientoRequest("4135", BigDecimal.ZERO, new BigDecimal(creditoVentas))));
    }

    @Test
    void registrar_asientoValido_quedaPersistidoConSusLineas() {
        AsientoResponse respuesta = servicio.registrar(ventaDeContado("500000.00"));

        assertThat(respuesta.id()).isNotNull();
        assertThat(contar("asiento")).isEqualTo(1);
        assertThat(contar("linea_asiento")).isEqualTo(2);
    }

    @Test
    void registrar_asientoDescuadrado_noDejaNadaEnLaBase() {
        assertThatThrownBy(() -> servicio.registrar(ventaDeContado("400000.00")))
                .isInstanceOf(ReglaNegocioException.class);

        assertThat(contar("asiento")).isZero();
        assertThat(contar("linea_asiento")).isZero();
    }

    /**
     * ATOMICIDAD REAL: la línea 1 se inserta bien, la línea 2 viola una restricción CHECK
     * de la base de datos (débito negativo), y TODO se revierte.
     * El servicio no detecta el problema (el asiento está "balanceado": 150-100 = 50 = 50),
     * por eso el fallo ocurre en la base, después de que ya se insertó el encabezado y la línea 1.
     */
    @Test
    void registrar_fallaALaMitad_haceRollbackDeTodo() {
        AsientoRequest conLineaInvalida = new AsientoRequest(LocalDate.of(2026, 11, 10), "Asiento con línea inválida", List.of(
                new LineaAsientoRequest("1105", new BigDecimal("150.00"), BigDecimal.ZERO),
                new LineaAsientoRequest("4135", new BigDecimal("-100.00"), new BigDecimal("50.00"))));

        assertThatThrownBy(() -> servicio.registrar(conLineaInvalida))
                .isInstanceOf(DataAccessException.class);

        assertThat(contar("asiento")).isZero();        // ni el encabezado quedó
        assertThat(contar("linea_asiento")).isZero();  // ni la línea que sí era válida
    }

    @Test
    void baseDeDatos_rechazaPorSiSolaUnaLineaConDebitoYCredito() {
        jdbc.update("INSERT INTO asiento (fecha, descripcion) VALUES (?, ?)", Date.valueOf("2026-11-10"), "Prueba directa");
        Long asientoId = jdbc.queryForObject("SELECT id FROM asiento", Long.class);
        Long cuentaId = jdbc.queryForObject("SELECT id FROM cuenta WHERE codigo = '1105'", Long.class);

        // Saltándonos todo el código Java, la restricción CHECK sigue protegiendo los datos
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO linea_asiento (asiento_id, cuenta_id, debito, credito) VALUES (?, ?, ?, ?)",
                asientoId, cuentaId, 100, 100))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void anular_persisteElNuevoEstado() {
        AsientoResponse creado = servicio.registrar(ventaDeContado("500000.00"));

        servicio.anular(creado.id());

        String estado = jdbc.queryForObject("SELECT estado FROM asiento WHERE id = ?", String.class, creado.id());
        assertThat(estado).isEqualTo("ANULADO");
    }
}
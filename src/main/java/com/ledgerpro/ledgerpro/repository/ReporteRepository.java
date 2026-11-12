package com.ledgerpro.ledgerpro.repository;

import com.ledgerpro.ledgerpro.domain.EstadoAsiento;
import com.ledgerpro.ledgerpro.domain.TipoCuenta;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Consultas de reportes en SQL puro. Solo lectura. */
@Repository
@RequiredArgsConstructor
public class ReporteRepository {

    private static final String CONTABILIZADO = EstadoAsiento.CONTABILIZADO.name();

    private final JdbcClient jdbc;

    /** Filas internas del repositorio (no son parte de la API pública). */
    public record SumaPorCuenta(String codigo, String nombre, TipoCuenta tipo,
                                BigDecimal debitos, BigDecimal creditos) {}

    public record MovimientoCrudo(LocalDate fecha, Long asientoId, String descripcion,
                                  BigDecimal debito, BigDecimal credito, BigDecimal netoAcumulado) {}

    /** Total de débitos y créditos por cuenta en el período (solo asientos contabilizados). */
    public List<SumaPorCuenta> sumasPorCuenta(LocalDate desde, LocalDate hasta) {
        return jdbc.sql("""
                SELECT c.codigo, c.nombre, c.tipo,
                       SUM(l.debito)  AS total_debitos,
                       SUM(l.credito) AS total_creditos
                FROM linea_asiento l
                JOIN asiento a ON a.id = l.asiento_id
                JOIN cuenta  c ON c.id = l.cuenta_id
                WHERE a.estado = :estado
                  AND a.fecha BETWEEN :desde AND :hasta
                GROUP BY c.codigo, c.nombre, c.tipo
                ORDER BY c.codigo
                """)
                .param("estado", CONTABILIZADO)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, fila) -> new SumaPorCuenta(
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        TipoCuenta.valueOf(rs.getString("tipo")),
                        rs.getBigDecimal("total_debitos"),
                        rs.getBigDecimal("total_creditos")))
                .list();
    }

    /** Débitos menos créditos de una cuenta ANTES de la fecha dada (saldo inicial del período). */
    public BigDecimal netoAntesDe(Long cuentaId, LocalDate fecha) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(l.debito - l.credito), 0)
                FROM linea_asiento l
                JOIN asiento a ON a.id = l.asiento_id
                WHERE l.cuenta_id = :cuentaId
                  AND a.estado = :estado
                  AND a.fecha < :fecha
                """)
                .param("cuentaId", cuentaId)
                .param("estado", CONTABILIZADO)
                .param("fecha", fecha)
                .query(BigDecimal.class)
                .single();
    }

    /** Movimientos de una cuenta con el neto acumulado calculado por una función de ventana. */
    public List<MovimientoCrudo> movimientos(Long cuentaId, LocalDate desde, LocalDate hasta) {
        return jdbc.sql("""
                SELECT a.fecha, a.id AS asiento_id, a.descripcion, l.debito, l.credito,
                       SUM(l.debito - l.credito) OVER (ORDER BY a.fecha, a.id, l.id) AS neto_acumulado
                FROM linea_asiento l
                JOIN asiento a ON a.id = l.asiento_id
                WHERE l.cuenta_id = :cuentaId
                  AND a.estado = :estado
                  AND a.fecha BETWEEN :desde AND :hasta
                ORDER BY a.fecha, a.id, l.id
                """)
                .param("cuentaId", cuentaId)
                .param("estado", CONTABILIZADO)
                .param("desde", desde)
                .param("hasta", hasta)
                .query((rs, fila) -> new MovimientoCrudo(
                        rs.getObject("fecha", LocalDate.class),
                        rs.getLong("asiento_id"),
                        rs.getString("descripcion"),
                        rs.getBigDecimal("debito"),
                        rs.getBigDecimal("credito"),
                        rs.getBigDecimal("neto_acumulado")))
                .list();
    }
}
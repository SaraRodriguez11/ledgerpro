package com.ledgerpro.ledgerpro.service;

import com.ledgerpro.ledgerpro.DTOs.AsientoRequest;
import com.ledgerpro.ledgerpro.DTOs.AsientoResponse;
import com.ledgerpro.ledgerpro.DTOs.LineaAsientoRequest;
import com.ledgerpro.ledgerpro.DTOs.reporte.BalanceComprobacion;
import com.ledgerpro.ledgerpro.DTOs.reporte.EstadoResultados;
import com.ledgerpro.ledgerpro.DTOs.reporte.LibroMayor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReporteServiceIntegracionTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 30);

    @Autowired private ReporteService reportes;
    @Autowired private AsientoService asientos;
    @Autowired private JdbcTemplate jdbc;
    @Value("${spring.datasource.url}") private String urlBaseDeDatos;

    @BeforeEach
    void limpiar() {
        assertThat(urlBaseDeDatos).endsWith("_test");
        jdbc.execute("DELETE FROM linea_asiento");
        jdbc.execute("DELETE FROM asiento");
    }

    /** Crea un asiento simple: debita una cuenta y acredita otra por el mismo monto. */
    private AsientoResponse asiento(String fecha, String descripcion,
                                    String cuentaDebito, String cuentaCredito, String monto) {
        BigDecimal valor = new BigDecimal(monto);
        return asientos.registrar(new AsientoRequest(LocalDate.parse(fecha), descripcion, List.of(
                new LineaAsientoRequest(cuentaDebito, valor, BigDecimal.ZERO),
                new LineaAsientoRequest(cuentaCredito, BigDecimal.ZERO, valor))));
    }

    @Test
    void balanceYEstadoDeResultados_reflejanLosAsientos() {
        asiento("2026-11-10", "Venta de contado", "1105", "4135", "500000.00");
        asiento("2026-11-15", "Pago de servicios", "5135", "1105", "200000.00");

        BalanceComprobacion balance = reportes.balanceComprobacion(DESDE, HASTA);
        assertThat(balance.cuadrado()).isTrue();
        assertThat(balance.totalDebitos()).isEqualByComparingTo("700000.00");
        assertThat(balance.totalCreditos()).isEqualByComparingTo("700000.00");

        EstadoResultados resultados = reportes.estadoResultados(DESDE, HASTA);
        assertThat(resultados.totalIngresos()).isEqualByComparingTo("500000.00");
        assertThat(resultados.totalGastos()).isEqualByComparingTo("200000.00");
        assertThat(resultados.utilidadNeta()).isEqualByComparingTo("300000.00");
    }

    @Test
    void libroMayor_calculaElSaldoAcumuladoMovimientoAMovimiento() {
        asiento("2026-11-10", "Venta de contado", "1105", "4135", "500000.00");
        asiento("2026-11-15", "Pago de servicios", "5135", "1105", "200000.00");

        LibroMayor mayorCaja = reportes.libroMayor("1105", DESDE, HASTA);

        assertThat(mayorCaja.movimientos()).hasSize(2);
        assertThat(mayorCaja.movimientos().get(0).saldo()).isEqualByComparingTo("500000.00");
        assertThat(mayorCaja.movimientos().get(1).saldo()).isEqualByComparingTo("300000.00");
        assertThat(mayorCaja.saldoFinal()).isEqualByComparingTo("300000.00");
    }

    @Test
    void libroMayor_incluyeElSaldoInicialDeFechasAnteriores() {
        asiento("2026-10-20", "Venta de octubre", "1105", "4135", "100000.00");
        asiento("2026-11-10", "Venta de noviembre", "1105", "4135", "50000.00");

        LibroMayor mayorCaja = reportes.libroMayor("1105", DESDE, HASTA);

        assertThat(mayorCaja.saldoInicial()).isEqualByComparingTo("100000.00");
        assertThat(mayorCaja.saldoFinal()).isEqualByComparingTo("150000.00");
    }

    @Test
    void losAsientosAnuladosNoCuentanEnLosReportes() {
        asiento("2026-11-10", "Venta de contado", "1105", "4135", "500000.00");
        AsientoResponse gasto = asiento("2026-11-15", "Pago de servicios", "5135", "1105", "200000.00");

        asientos.anular(gasto.id());

        EstadoResultados resultados = reportes.estadoResultados(DESDE, HASTA);
        assertThat(resultados.totalGastos()).isEqualByComparingTo("0");
        assertThat(resultados.utilidadNeta()).isEqualByComparingTo("500000.00");
    }
}
package com.ledgerpro.ledgerpro.service;

import com.ledgerpro.ledgerpro.domain.Cuenta;
import com.ledgerpro.ledgerpro.domain.TipoCuenta;
import com.ledgerpro.ledgerpro.DTOs.reporte.BalanceComprobacion;
import com.ledgerpro.ledgerpro.DTOs.reporte.EstadoResultados;
import com.ledgerpro.ledgerpro.DTOs.reporte.LibroMayor;
import com.ledgerpro.ledgerpro.DTOs.reporte.LineaBalance;
import com.ledgerpro.ledgerpro.exception.CuentaNoEncontradaException;
import com.ledgerpro.ledgerpro.exception.ReglaNegocioException;
import com.ledgerpro.ledgerpro.repository.CuentaRepository;
import com.ledgerpro.ledgerpro.repository.ReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // todos los reportes solo leen
public class ReporteServiceImpl implements ReporteService {

    private final ReporteRepository reporteRepository;
    private final CuentaRepository cuentaRepository;

    @Override
    public BalanceComprobacion balanceComprobacion(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        List<LineaBalance> lineas = lineasBalance(desde, hasta);
        BigDecimal debitos = sumar(lineas, LineaBalance::totalDebitos);
        BigDecimal creditos = sumar(lineas, LineaBalance::totalCreditos);
        return new BalanceComprobacion(desde, hasta, lineas, debitos, creditos,
                debitos.compareTo(creditos) == 0);
    }

    @Override
    public EstadoResultados estadoResultados(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        List<LineaBalance> lineas = lineasBalance(desde, hasta);
        List<LineaBalance> ingresos = lineas.stream().filter(l -> l.tipo() == TipoCuenta.INGRESO).toList();
        List<LineaBalance> gastos = lineas.stream().filter(l -> l.tipo() == TipoCuenta.GASTO).toList();

        BigDecimal totalIngresos = sumar(ingresos, LineaBalance::saldo);
        BigDecimal totalGastos = sumar(gastos, LineaBalance::saldo);
        return new EstadoResultados(desde, hasta, ingresos, gastos,
                totalIngresos, totalGastos, totalIngresos.subtract(totalGastos));
    }

    @Override
    public LibroMayor libroMayor(String codigoCuenta, LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        Cuenta cuenta = cuentaRepository.findByCodigo(codigoCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(codigoCuenta));

        // Las consultas trabajan en "neto deudor" (débitos - créditos).
        // Para cuentas acreedoras se invierte el signo al presentar el saldo.
        BigDecimal factor = cuenta.getTipo().esDeudora() ? BigDecimal.ONE : BigDecimal.ONE.negate();
        BigDecimal netoInicial = reporteRepository.netoAntesDe(cuenta.getId(), desde);

        List<LibroMayor.Movimiento> movimientos = reporteRepository
                .movimientos(cuenta.getId(), desde, hasta).stream()
                .map(m -> new LibroMayor.Movimiento(m.fecha(), m.asientoId(), m.descripcion(),
                        m.debito(), m.credito(),
                        factor.multiply(netoInicial.add(m.netoAcumulado()))))
                .toList();

        BigDecimal saldoInicial = factor.multiply(netoInicial);
        BigDecimal saldoFinal = movimientos.isEmpty()
                ? saldoInicial
                : movimientos.get(movimientos.size() - 1).saldo();

        return new LibroMayor(cuenta.getCodigo(), cuenta.getNombre(), cuenta.getTipo(),
                desde, hasta, saldoInicial, movimientos, saldoFinal);
    }

    private List<LineaBalance> lineasBalance(LocalDate desde, LocalDate hasta) {
        return reporteRepository.sumasPorCuenta(desde, hasta).stream()
                .map(s -> new LineaBalance(s.codigo(), s.nombre(), s.tipo(), s.debitos(), s.creditos(),
                        s.tipo().calcularSaldo(s.debitos(), s.creditos())))
                .toList();
    }

    private BigDecimal sumar(List<LineaBalance> lineas, Function<LineaBalance, BigDecimal> campo) {
        return lineas.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }
}
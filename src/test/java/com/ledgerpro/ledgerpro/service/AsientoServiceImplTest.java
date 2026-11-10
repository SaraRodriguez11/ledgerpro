package com.ledgerpro.ledgerpro.service;

import com.ledgerpro.ledgerpro.domain.Asiento;
import com.ledgerpro.ledgerpro.domain.Cuenta;
import com.ledgerpro.ledgerpro.domain.EstadoAsiento;
import com.ledgerpro.ledgerpro.domain.TipoCuenta;
import com.ledgerpro.ledgerpro.DTOs.AsientoRequest;
import com.ledgerpro.ledgerpro.DTOs.AsientoResponse;
import com.ledgerpro.ledgerpro.DTOs.LineaAsientoRequest;
import com.ledgerpro.ledgerpro.exception.AsientoNoEncontradoException;
import com.ledgerpro.ledgerpro.exception.CuentaNoEncontradaException;
import com.ledgerpro.ledgerpro.exception.ReglaNegocioException;
import com.ledgerpro.ledgerpro.repository.AsientoRepository;
import com.ledgerpro.ledgerpro.repository.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias: el servicio se prueba AISLADO, con los repositorios simulados (mocks). */
@ExtendWith(MockitoExtension.class)
class AsientoServiceImplTest {

    @Mock private AsientoRepository asientoRepository;
    @Mock private CuentaRepository cuentaRepository;
    @InjectMocks private AsientoServiceImpl servicio; // Mockito le inyecta los mocks por constructor

    private Cuenta caja;
    private Cuenta ventas;

    @BeforeEach
    void preparar() {
        caja = new Cuenta("1105", "Caja", TipoCuenta.ACTIVO, null);
        ventas = new Cuenta("4135", "Ventas", TipoCuenta.INGRESO, null);
    }

    private AsientoRequest ventaDeContado(String creditoVentas) {
        return new AsientoRequest(LocalDate.of(2026, 11, 10), "Venta de contado", List.of(
                new LineaAsientoRequest("1105", new BigDecimal("500000.00"), BigDecimal.ZERO),
                new LineaAsientoRequest("4135", BigDecimal.ZERO, new BigDecimal(creditoVentas))));
    }

    @Test
    void registrar_asientoBalanceado_loGuardaYDevuelveTotalesIguales() {
        when(cuentaRepository.findByCodigo("1105")).thenReturn(Optional.of(caja));
        when(cuentaRepository.findByCodigo("4135")).thenReturn(Optional.of(ventas));
        // save() devuelve el mismo objeto que recibe
        when(asientoRepository.save(any(Asiento.class))).thenAnswer(inv -> inv.getArgument(0));

        AsientoResponse respuesta = servicio.registrar(ventaDeContado("500000.00"));

        assertThat(respuesta.totalDebitos()).isEqualByComparingTo("500000.00");
        assertThat(respuesta.totalCreditos()).isEqualByComparingTo("500000.00");
        assertThat(respuesta.lineas()).hasSize(2);
        verify(asientoRepository).save(any(Asiento.class));
    }

    @Test
    void registrar_asientoDescuadrado_lanzaReglaDeNegocioYNoGuarda() {
        when(cuentaRepository.findByCodigo("1105")).thenReturn(Optional.of(caja));
        when(cuentaRepository.findByCodigo("4135")).thenReturn(Optional.of(ventas));

        assertThatThrownBy(() -> servicio.registrar(ventaDeContado("400000.00")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("descuadrado");

        verify(asientoRepository, never()).save(any());
    }

    @Test
    void registrar_cuentaInexistente_lanzaCuentaNoEncontradaYNoGuarda() {
        when(cuentaRepository.findByCodigo("1105")).thenReturn(Optional.of(caja));
        when(cuentaRepository.findByCodigo("4135")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(ventaDeContado("500000.00")))
                .isInstanceOf(CuentaNoEncontradaException.class);

        verify(asientoRepository, never()).save(any());
    }

    @Test
    void registrar_cuentaInactiva_lanzaReglaDeNegocio() {
        caja.desactivar();
        when(cuentaRepository.findByCodigo("1105")).thenReturn(Optional.of(caja));

        assertThatThrownBy(() -> servicio.registrar(ventaDeContado("500000.00")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("inactiva");
    }

    @Test
    void registrar_lineaConDebitoYCredito_seRechaza() {
        AsientoRequest invalido = new AsientoRequest(LocalDate.of(2026, 11, 10), "Línea inválida", List.of(
                new LineaAsientoRequest("1105", new BigDecimal("100"), new BigDecimal("100")),
                new LineaAsientoRequest("4135", BigDecimal.ZERO, new BigDecimal("100"))));

        assertThatThrownBy(() -> servicio.registrar(invalido))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("débito o crédito");
    }

    @Test
    void anular_asientoContabilizado_cambiaSuEstado() {
        Asiento asiento = new Asiento(LocalDate.of(2026, 11, 10), "Venta");
        when(asientoRepository.findById(1L)).thenReturn(Optional.of(asiento));

        AsientoResponse respuesta = servicio.anular(1L);

        assertThat(respuesta.estado()).isEqualTo(EstadoAsiento.ANULADO);
    }

    @Test
    void anular_asientoYaAnulado_lanzaReglaDeNegocio() {
        Asiento asiento = new Asiento(LocalDate.of(2026, 11, 10), "Venta");
        asiento.anular();
        when(asientoRepository.findById(1L)).thenReturn(Optional.of(asiento));

        assertThatThrownBy(() -> servicio.anular(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya está anulado");
    }

    @Test
    void buscarPorId_inexistente_lanzaAsientoNoEncontrado() {
        when(asientoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscarPorId(99L))
                .isInstanceOf(AsientoNoEncontradoException.class);
    }
}
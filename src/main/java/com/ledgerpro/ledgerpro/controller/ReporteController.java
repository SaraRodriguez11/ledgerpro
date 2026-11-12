package com.ledgerpro.ledgerpro.controller;

import com.ledgerpro.ledgerpro.DTOs.reporte.BalanceComprobacion;
import com.ledgerpro.ledgerpro.DTOs.reporte.EstadoResultados;
import com.ledgerpro.ledgerpro.DTOs.reporte.LibroMayor;
import com.ledgerpro.ledgerpro.service.ReporteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Balance de comprobación, estado de resultados y libro mayor")
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/balance-comprobacion")
    public BalanceComprobacion balance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.balanceComprobacion(desde, hasta);
    }

    @GetMapping("/estado-resultados")
    public EstadoResultados estadoResultados(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.estadoResultados(desde, hasta);
    }

    @GetMapping("/libro-mayor/{codigoCuenta}")
    public LibroMayor libroMayor(
            @PathVariable String codigoCuenta,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.libroMayor(codigoCuenta, desde, hasta);
    }
}
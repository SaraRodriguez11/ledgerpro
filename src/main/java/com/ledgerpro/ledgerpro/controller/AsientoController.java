package com.ledgerpro.ledgerpro.controller;

import com.ledgerpro.ledgerpro.DTOs.AsientoRequest;
import com.ledgerpro.ledgerpro.DTOs.AsientoResponse;
import com.ledgerpro.ledgerpro.service.AsientoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Capa web: recibe HTTP, delega en el servicio y devuelve la respuesta. Sin lógica de negocio. */
@RestController
@RequestMapping("/api/asientos")
@RequiredArgsConstructor
@Tag(name = "Asientos", description = "Registro y consulta de asientos contables")
public class AsientoController {

    // Depende de la INTERFAZ, no de la implementación (principio D de SOLID)
    private final AsientoService asientoService;

    @PostMapping
    public ResponseEntity<AsientoResponse> registrar(@Valid @RequestBody AsientoRequest request) {
        AsientoResponse creado = asientoService.registrar(request);
        // 201 Created + cabecera Location con la URL del nuevo recurso
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping("/{id}")
    public AsientoResponse buscarPorId(@PathVariable Long id) {
        return asientoService.buscarPorId(id);
    }

    // PATCH porque modifica parcialmente el recurso (cambia solo su estado)
    @PatchMapping("/{id}/anular")
    public AsientoResponse anular(@PathVariable Long id) {
        return asientoService.anular(id);
    }
}
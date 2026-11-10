package com.ledgerpro.ledgerpro.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record AsientoRequest(
        @NotNull LocalDate fecha,
        @NotBlank @Size(max = 255) String descripcion,
        @NotNull @Size(min = 2, message = "Un asiento necesita al menos 2 líneas")
        List<@Valid LineaAsientoRequest> lineas
) {}
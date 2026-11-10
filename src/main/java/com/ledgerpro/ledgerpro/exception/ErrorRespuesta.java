package com.ledgerpro.ledgerpro.exception;

import java.time.Instant;
import java.util.List;

/** Formato único de error para toda la API. */
public record ErrorRespuesta(
        Instant fecha,
        int estado,
        String error,
        String mensaje,
        List<String> detalles
) {}
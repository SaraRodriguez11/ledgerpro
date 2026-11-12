package com.ledgerpro.ledgerpro.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/** Traduce excepciones a respuestas HTTP coherentes para todos los controladores. */
@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones {

    // 404: el recurso pedido no existe
    @ExceptionHandler({CuentaNoEncontradaException.class, AsientoNoEncontradoException.class})
    public ResponseEntity<ErrorRespuesta> noEncontrado(RuntimeException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    // 422: la petición es válida en formato pero viola una regla contable
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuesta> reglaNegocio(ReglaNegocioException ex) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of());
    }

    // 400: falló Bean Validation en el DTO (@NotBlank, @NotNull, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", detalles);
    }

    // 400: JSON mal formado o con tipos incorrectos (por ejemplo, una fecha inválida)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> jsonIlegible(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la petición es ilegible o tiene un formato incorrecto", List.of());
    }

    // 500: cualquier error inesperado. Se registra en el log pero NO se expone el detalle al cliente.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception ex) {
        log.error("Error inesperado", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }
    // 400: falta un parámetro o tiene formato incorrecto (por ejemplo, una fecha mal escrita)
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorRespuesta> parametroInvalido(Exception ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "Parámetros ausentes o con formato incorrecto (las fechas van como AAAA-MM-DD)", List.of());
    }

    // 404: la URL no existe
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException ex) {
        return construir(HttpStatus.NOT_FOUND, "Recurso no encontrado", List.of());
    }

    // 405: el método HTTP no está permitido en esa URL
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido para esta ruta", List.of());
    }

    private ResponseEntity<ErrorRespuesta> construir(HttpStatus estado, String mensaje, List<String> detalles) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(
                Instant.now(), estado.value(), estado.getReasonPhrase(), mensaje, detalles);
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
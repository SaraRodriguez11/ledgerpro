package com.ledgerpro.ledgerpro.exception;

public class AsientoNoEncontradoException extends RuntimeException {
    public AsientoNoEncontradoException(Long id) {
        super("No existe el asiento con id " + id);
    }
}
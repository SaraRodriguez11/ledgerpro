package com.ledgerpro.ledgerpro.exception;

public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(String codigo) {
        super("No existe la cuenta con código " + codigo);
    }
}
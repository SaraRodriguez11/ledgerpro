package com.ledgerpro.ledgerpro.service;

import com.ledgerpro.ledgerpro.DTOs.AsientoRequest;
import com.ledgerpro.ledgerpro.DTOs.AsientoResponse;

public interface AsientoService {

    AsientoResponse registrar(AsientoRequest request);

    AsientoResponse buscarPorId(Long id);

    AsientoResponse anular(Long id);
}

package com.ledgerpro.ledgerpro.service;
import com.ledgerpro.ledgerpro.domain.Asiento;
import com.ledgerpro.ledgerpro.domain.Cuenta;
import com.ledgerpro.ledgerpro.domain.EstadoAsiento;
import com.ledgerpro.ledgerpro.DTOs.AsientoRequest;
import com.ledgerpro.ledgerpro.DTOs.AsientoResponse;
import com.ledgerpro.ledgerpro.DTOs.LineaAsientoRequest;
import com.ledgerpro.ledgerpro.exception.AsientoNoEncontradoException;
import com.ledgerpro.ledgerpro.exception.CuentaNoEncontradaException;
import com.ledgerpro.ledgerpro.exception.ReglaNegocioException;
import com.ledgerpro.ledgerpro.repository.AsientoRepository;
import com.ledgerpro.ledgerpro.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // genera el constructor con los campos final: inyección por constructor
public class AsientoServiceImpl implements AsientoService {

    private final AsientoRepository asientoRepository;
    private final CuentaRepository cuentaRepository;

    /**
     * Registra un asiento con partida doble.
     * ATOMICIDAD (la A de ACID): o se guardan el asiento y TODAS sus líneas,
     * o no se guarda nada. Si se lanza cualquier excepción, Spring hace rollback.
     */
    @Override
    @Transactional
    public AsientoResponse registrar(AsientoRequest request) {
        Asiento asiento = new Asiento(request.fecha(), request.descripcion());

        for (LineaAsientoRequest linea : request.lineas()) {
            validarUnSoloLado(linea);

            Cuenta cuenta = cuentaRepository.findByCodigo(linea.codigoCuenta())
                    .orElseThrow(() -> new CuentaNoEncontradaException(linea.codigoCuenta()));

            if (!cuenta.isActiva()) {
                throw new ReglaNegocioException("La cuenta " + cuenta.getCodigo() + " está inactiva");
            }
            asiento.agregarLinea(cuenta, linea.debito(), linea.credito());
        }

        // Regla de partida doble: la lógica vive en el dominio, el servicio la hace cumplir
        if (!asiento.estaBalanceado()) {
            throw new ReglaNegocioException(String.format(
                    "Asiento descuadrado: débitos %s distintos de créditos %s",
                    asiento.totalDebitos(), asiento.totalCreditos()));
        }

        // El map a DTO se hace DENTRO de la transacción: con open-in-view=false,
        // acceder a relaciones LAZY fuera de ella lanza LazyInitializationException
        return AsientoResponse.desde(asientoRepository.save(asiento));
    }

    @Override
    @Transactional(readOnly = true) // optimiza y deja claro que no modifica datos
    public AsientoResponse buscarPorId(Long id) {
        return AsientoResponse.desde(obtener(id));
    }

    @Override
    @Transactional
    public AsientoResponse anular(Long id) {
        Asiento asiento = obtener(id);
        if (asiento.getEstado() == EstadoAsiento.ANULADO) {
            throw new ReglaNegocioException("El asiento " + id + " ya está anulado");
        }
        asiento.anular();
        // No hace falta save(): JPA detecta el cambio (dirty checking) y hace el UPDATE al confirmar
        return AsientoResponse.desde(asiento);
    }

    private Asiento obtener(Long id) {
        return asientoRepository.findById(id)
                .orElseThrow(() -> new AsientoNoEncontradoException(id));
    }

    /** Cada línea debe ser débito o crédito, nunca ambos ni ninguno. */
    private void validarUnSoloLado(LineaAsientoRequest linea) {
        boolean tieneDebito = linea.debito().signum() > 0;
        boolean tieneCredito = linea.credito().signum() > 0;
        if (tieneDebito == tieneCredito) {
            throw new ReglaNegocioException(
                    "La línea de la cuenta " + linea.codigoCuenta() + " debe tener débito o crédito, no ambos ni ninguno");
        }
    }
}
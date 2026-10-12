package cl.recetaya.inventario.servicio;

import cl.recetaya.inventario.excepcion.CodigoRechazoReserva;
import cl.recetaya.inventario.excepcion.ExcepcionReserva;
import cl.recetaya.inventario.transferencia.RespuestaReserva;
import cl.recetaya.inventario.transferencia.SolicitudReserva;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Supplier;

@Service
public class ServicioReservas {
    private final OperacionesReserva operaciones;

    public ServicioReservas(OperacionesReserva operaciones) {
        this.operaciones = operaciones;
    }

    public RespuestaReserva reservar(SolicitudReserva solicitud) {
        for (int intento = 1; intento <= 3; intento++) {
            try {
                return operaciones.reservar(solicitud);
            } catch (DataIntegrityViolationException excepcion) {
                if (solicitud != null && solicitud.recetaId() != null) {
                    try {
                        return operaciones.consultarReintento(solicitud);
                    } catch (ExcepcionReserva conflicto) {
                        if (conflicto.obtenerCodigo() != CodigoRechazoReserva.CONFLICTO_CONCURRENTE) throw conflicto;
                    }
                }
                throw excepcion;
            } catch (PessimisticLockingFailureException excepcion) {
                if (intento == 3) throw conflictoConcurrente();
            }
        }
        throw conflictoConcurrente();
    }

    public RespuestaReserva liberar(UUID recetaId) {
        return repetirSiHayBloqueo(() -> operaciones.liberar(recetaId));
    }

    public RespuestaReserva consumir(UUID recetaId) {
        return repetirSiHayBloqueo(() -> operaciones.consumir(recetaId));
    }

    public RespuestaReserva consultar(UUID recetaId) {
        return operaciones.consultar(recetaId);
    }

    private RespuestaReserva repetirSiHayBloqueo(Supplier<RespuestaReserva> operacion) {
        for (int intento = 1; intento <= 3; intento++) {
            try {
                return operacion.get();
            } catch (PessimisticLockingFailureException excepcion) {
                if (intento == 3) throw conflictoConcurrente();
            }
        }
        throw conflictoConcurrente();
    }

    private ExcepcionReserva conflictoConcurrente() {
        return new ExcepcionReserva(CodigoRechazoReserva.CONFLICTO_CONCURRENTE,
                "La reserva está ocupada; reintente la operación.");
    }
}

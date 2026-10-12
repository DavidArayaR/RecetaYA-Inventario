package cl.recetaya.inventario.integracion;

import cl.recetaya.inventario.excepcion.CodigoRechazoReserva;
import cl.recetaya.inventario.excepcion.ExcepcionConflictoDatos;
import cl.recetaya.inventario.excepcion.ExcepcionReserva;
import cl.recetaya.inventario.repositorio.RepositorioMensajeSalida;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class ProcesadorEventosInventario {
    private final CodificadorEventos codificador;
    private final ProcesamientoTransaccionalInventario transacciones;
    private final RepositorioMensajeSalida salidas;

    public ProcesadorEventosInventario(CodificadorEventos codificador,
                                      ProcesamientoTransaccionalInventario transacciones,
                                      RepositorioMensajeSalida salidas) {
        this.codificador = codificador;
        this.transacciones = transacciones;
        this.salidas = salidas;
    }

    public void procesar(String contenido) {
        EventoSolicitudInventario solicitud = codificador.leerSolicitud(contenido);
        if (solicitud == null || solicitud.mensajeId() == null) {
            throw new ExcepcionReserva(CodigoRechazoReserva.SOLICITUD_INVALIDA,
                    "El mensaje requiere un identificador de solicitud.");
        }
        if (solicitud.correlacionId() == null || solicitud.tipoOperacion() == null
                || solicitud.recetaId() == null || solicitud.fechaHora() == null) {
            transacciones.registrarRechazo(solicitud, CodigoRechazoReserva.SOLICITUD_INVALIDA,
                    "El mensaje requiere correlación, operación, receta y fecha.");
            return;
        }
        try {
            transacciones.procesar(solicitud);
        } catch (ExcepcionReserva excepcion) {
            if (excepcion.obtenerCodigo() == CodigoRechazoReserva.CONFLICTO_CONCURRENTE) throw excepcion;
            transacciones.registrarRechazo(solicitud, excepcion.obtenerCodigo(), excepcion.getMessage());
        } catch (ExcepcionConflictoDatos excepcion) {
            transacciones.registrarRechazo(solicitud, CodigoRechazoReserva.INCONSISTENCIA_EXISTENCIAS,
                    excepcion.getMessage());
        } catch (DataIntegrityViolationException excepcion) {
            if (salidas.buscarPorSolicitud(solicitud.mensajeId()).isEmpty()) throw excepcion;
        } catch (PessimisticLockingFailureException excepcion) {
            throw excepcion;
        }
    }
}

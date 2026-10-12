package cl.recetaya.inventario.integracion;

import cl.recetaya.inventario.entidad.MensajeSalida;
import cl.recetaya.inventario.excepcion.CodigoRechazoReserva;
import cl.recetaya.inventario.repositorio.RepositorioMensajeSalida;
import cl.recetaya.inventario.servicio.OperacionesReserva;
import cl.recetaya.inventario.transferencia.RespuestaReserva;
import cl.recetaya.inventario.transferencia.SolicitudReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProcesamientoTransaccionalInventario {
    private final OperacionesReserva operaciones;
    private final RepositorioMensajeSalida salidas;
    private final CodificadorEventos codificador;

    public ProcesamientoTransaccionalInventario(OperacionesReserva operaciones,
                                               RepositorioMensajeSalida salidas,
                                               CodificadorEventos codificador) {
        this.operaciones = operaciones;
        this.salidas = salidas;
        this.codificador = codificador;
    }

    @Transactional
    public void procesar(EventoSolicitudInventario solicitud) {
        if (salidas.buscarPorSolicitud(solicitud.mensajeId()).isPresent()) return;
        RespuestaReserva reserva = switch (solicitud.tipoOperacion()) {
            case RESERVAR -> operaciones.reservar(new SolicitudReserva(
                    solicitud.recetaId(), solicitud.sucursalId(), solicitud.medicamentos()));
            case LIBERAR -> operaciones.liberar(solicitud.recetaId());
            case CONSUMIR -> operaciones.consumir(solicitud.recetaId());
        };
        guardarResultado(solicitud, ResultadoOperacion.CONFIRMADA, null, null, reserva);
    }

    @Transactional
    public void registrarRechazo(EventoSolicitudInventario solicitud,
                                CodigoRechazoReserva codigo, String mensaje) {
        if (salidas.buscarPorSolicitud(solicitud.mensajeId()).isPresent()) return;
        guardarResultado(solicitud, ResultadoOperacion.RECHAZADA, codigo, mensaje, null);
    }

    private void guardarResultado(EventoSolicitudInventario solicitud, ResultadoOperacion resultado,
                                  CodigoRechazoReserva codigo, String mensaje, RespuestaReserva reserva) {
        UUID eventoId = UUID.randomUUID();
        EventoResultadoInventario evento = new EventoResultadoInventario(eventoId, solicitud.mensajeId(),
                solicitud.correlacionId(), solicitud.tipoOperacion(), solicitud.recetaId(),
                resultado, codigo, mensaje, reserva, Instant.now());
        salidas.saveAndFlush(new MensajeSalida(eventoId, solicitud.mensajeId(),
                codificador.escribirResultado(evento)));
    }
}

package cl.recetaya.inventario.integracion;

import cl.recetaya.inventario.excepcion.CodigoRechazoReserva;
import cl.recetaya.inventario.transferencia.RespuestaReserva;

import java.time.Instant;
import java.util.UUID;

public record EventoResultadoInventario(
        UUID eventoId,
        UUID solicitudId,
        UUID correlacionId,
        TipoOperacionInventario tipoOperacion,
        UUID recetaId,
        ResultadoOperacion resultado,
        CodigoRechazoReserva codigoRechazo,
        String mensaje,
        RespuestaReserva reserva,
        Instant fechaHora) {
}

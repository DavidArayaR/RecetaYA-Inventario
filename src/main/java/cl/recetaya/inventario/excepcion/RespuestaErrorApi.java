package cl.recetaya.inventario.excepcion;

import java.time.Instant;

public record RespuestaErrorApi(
        Instant fechaHora,
        int codigoEstado,
        String error,
        String mensaje,
        String ruta) {
}

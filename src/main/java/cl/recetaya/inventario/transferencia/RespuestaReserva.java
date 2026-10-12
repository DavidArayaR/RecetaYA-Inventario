package cl.recetaya.inventario.transferencia;

import cl.recetaya.inventario.entidad.EstadoReserva;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RespuestaReserva(
        UUID id,
        UUID recetaId,
        UUID sucursalId,
        EstadoReserva estado,
        List<DetalleReserva> medicamentos,
        Instant fechaCreacion,
        Instant fechaActualizacion) {
}

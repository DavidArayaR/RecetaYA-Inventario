package cl.recetaya.inventario.transferencia;

import java.util.UUID;

public record RespuestaExistencias(
        UUID sucursalId,
        UUID medicamentoId,
        String nombreMedicamento,
        int cantidadFisica,
        int cantidadReservada,
        int cantidadDisponible) {
}

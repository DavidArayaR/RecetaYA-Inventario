package cl.recetaya.inventario.transferencia;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SolicitudActualizacionExistencias(@NotNull @Min(0) Integer cantidadFisica) {
}

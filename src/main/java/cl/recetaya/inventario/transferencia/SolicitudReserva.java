package cl.recetaya.inventario.transferencia;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SolicitudReserva(
        @NotNull UUID recetaId,
        @NotNull UUID sucursalId,
        @NotEmpty List<@Valid MedicamentoSolicitado> medicamentos) {
}

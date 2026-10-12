package cl.recetaya.inventario.transferencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudMedicamento(
        @NotBlank @Size(max = 50) String codigo,
        @NotBlank @Size(max = 160) String nombre,
        @Size(max = 100) String presentacion) {
}

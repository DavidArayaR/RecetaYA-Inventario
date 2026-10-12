package cl.recetaya.inventario.transferencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudSucursal(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 255) String direccion) {
}

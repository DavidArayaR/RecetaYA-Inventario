package cl.recetaya.inventario.transferencia;

import java.util.UUID;

public record DetalleReserva(UUID medicamentoId, int cantidad) {
}

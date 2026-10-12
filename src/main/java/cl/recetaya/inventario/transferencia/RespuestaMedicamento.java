package cl.recetaya.inventario.transferencia;

import java.util.UUID;

public record RespuestaMedicamento(UUID id, String codigo, String nombre, String presentacion) {
}

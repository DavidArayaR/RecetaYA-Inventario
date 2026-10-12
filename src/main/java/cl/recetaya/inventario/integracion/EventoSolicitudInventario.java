package cl.recetaya.inventario.integracion;

import cl.recetaya.inventario.transferencia.MedicamentoSolicitado;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventoSolicitudInventario(
        UUID mensajeId,
        UUID correlacionId,
        TipoOperacionInventario tipoOperacion,
        UUID recetaId,
        UUID sucursalId,
        List<MedicamentoSolicitado> medicamentos,
        Instant fechaHora) {
}

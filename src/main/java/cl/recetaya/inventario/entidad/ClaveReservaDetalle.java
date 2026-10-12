package cl.recetaya.inventario.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ClaveReservaDetalle implements Serializable {
    @Column(name = "reserva_id", nullable = false)
    private UUID reservaId;

    @Column(name = "medicamento_id", nullable = false)
    private UUID medicamentoId;

    protected ClaveReservaDetalle() {
    }

    public ClaveReservaDetalle(UUID reservaId, UUID medicamentoId) {
        this.reservaId = reservaId;
        this.medicamentoId = medicamentoId;
    }

    public UUID obtenerReservaId() { return reservaId; }
    public UUID obtenerMedicamentoId() { return medicamentoId; }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof ClaveReservaDetalle otraClave)) return false;
        return Objects.equals(reservaId, otraClave.reservaId)
                && Objects.equals(medicamentoId, otraClave.medicamentoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservaId, medicamentoId);
    }
}

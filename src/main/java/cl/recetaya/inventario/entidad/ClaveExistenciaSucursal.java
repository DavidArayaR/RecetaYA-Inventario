package cl.recetaya.inventario.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ClaveExistenciaSucursal implements Serializable {
    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;

    @Column(name = "medicamento_id", nullable = false)
    private UUID medicamentoId;

    protected ClaveExistenciaSucursal() {
    }

    public ClaveExistenciaSucursal(UUID sucursalId, UUID medicamentoId) {
        this.sucursalId = sucursalId;
        this.medicamentoId = medicamentoId;
    }

    public UUID obtenerIdentificadorSucursal() { return sucursalId; }
    public UUID obtenerIdentificadorMedicamento() { return medicamentoId; }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof ClaveExistenciaSucursal otraClave)) return false;
        return Objects.equals(sucursalId, otraClave.sucursalId)
                && Objects.equals(medicamentoId, otraClave.medicamentoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sucursalId, medicamentoId);
    }
}

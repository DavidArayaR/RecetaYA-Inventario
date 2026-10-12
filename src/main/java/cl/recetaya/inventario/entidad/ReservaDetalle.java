package cl.recetaya.inventario.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva_detalle")
public class ReservaDetalle {
    @EmbeddedId
    private ClaveReservaDetalle id;

    @MapsId("reservaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private ReservaExistencias reserva;

    @MapsId("medicamentoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Column(nullable = false)
    private int cantidad;

    protected ReservaDetalle() {
    }

    public ReservaDetalle(ReservaExistencias reserva, Medicamento medicamento, int cantidad) {
        id = new ClaveReservaDetalle(reserva.obtenerId(), medicamento.obtenerId());
        this.reserva = reserva;
        this.medicamento = medicamento;
        this.cantidad = cantidad;
    }

    public ClaveReservaDetalle obtenerId() { return id; }
    public ReservaExistencias obtenerReserva() { return reserva; }
    public Medicamento obtenerMedicamento() { return medicamento; }
    public int obtenerCantidad() { return cantidad; }
}

package cl.recetaya.inventario.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import cl.recetaya.inventario.excepcion.ExcepcionConflictoDatos;

@Entity
@Table(name = "existencia_sucursal")
public class ExistenciaSucursal {
    @EmbeddedId
    private ClaveExistenciaSucursal id;

    @MapsId("sucursalId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @MapsId("medicamentoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @Column(name = "cantidad_fisica", nullable = false)
    private int cantidadFisica;

    @Column(name = "cantidad_reservada", nullable = false)
    private int cantidadReservada;

    protected ExistenciaSucursal() {
    }

    public ExistenciaSucursal(Sucursal sucursal, Medicamento medicamento, int cantidadFisica) {
        this.id = new ClaveExistenciaSucursal(sucursal.obtenerId(), medicamento.obtenerId());
        this.sucursal = sucursal;
        this.medicamento = medicamento;
        this.cantidadFisica = cantidadFisica;
        this.cantidadReservada = 0;
    }

    public ClaveExistenciaSucursal obtenerId() { return id; }
    public Sucursal obtenerSucursal() { return sucursal; }
    public Medicamento obtenerMedicamento() { return medicamento; }
    public int obtenerCantidadFisica() { return cantidadFisica; }
    public void asignarCantidadFisica(int cantidadFisica) { this.cantidadFisica = cantidadFisica; }
    public int obtenerCantidadReservada() { return cantidadReservada; }
    public int obtenerCantidadDisponible() { return cantidadFisica - cantidadReservada; }

    public void reservar(int cantidad) {
        if (cantidad <= 0 || cantidad > obtenerCantidadDisponible()) {
            throw new ExcepcionConflictoDatos("Existencias insuficientes para reservar el medicamento.");
        }
        cantidadReservada += cantidad;
    }

    public void liberar(int cantidad) {
        if (cantidad <= 0 || cantidad > cantidadReservada) {
            throw new ExcepcionConflictoDatos("La reserva excede las existencias reservadas.");
        }
        cantidadReservada -= cantidad;
    }

    public void consumir(int cantidad) {
        if (cantidad <= 0 || cantidad > cantidadReservada || cantidad > cantidadFisica) {
            throw new ExcepcionConflictoDatos("La reserva no puede consumirse por inconsistencia de existencias.");
        }
        cantidadReservada -= cantidad;
        cantidadFisica -= cantidad;
    }
}

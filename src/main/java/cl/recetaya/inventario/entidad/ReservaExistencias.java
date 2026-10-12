package cl.recetaya.inventario.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "reserva_existencias")
public class ReservaExistencias {
    @Id
    private UUID id;

    @Column(name = "receta_id", nullable = false, unique = true)
    private UUID recetaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EstadoReserva estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    protected ReservaExistencias() {
    }

    public ReservaExistencias(UUID id, UUID recetaId, Sucursal sucursal) {
        this.id = id;
        this.recetaId = recetaId;
        this.sucursal = sucursal;
        this.estado = EstadoReserva.RESERVADA;
        this.fechaCreacion = LocalDateTime.now(ZoneOffset.UTC);
        this.fechaActualizacion = fechaCreacion;
    }

    public UUID obtenerId() { return id; }
    public UUID obtenerRecetaId() { return recetaId; }
    public Sucursal obtenerSucursal() { return sucursal; }
    public EstadoReserva obtenerEstado() { return estado; }
    public LocalDateTime obtenerFechaCreacion() { return fechaCreacion; }
    public LocalDateTime obtenerFechaActualizacion() { return fechaActualizacion; }

    public void cambiarEstado(EstadoReserva nuevoEstado) {
        estado = nuevoEstado;
        fechaActualizacion = LocalDateTime.now(ZoneOffset.UTC);
    }
}

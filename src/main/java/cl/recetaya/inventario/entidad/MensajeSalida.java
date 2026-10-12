package cl.recetaya.inventario.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "mensaje_salida")
public class MensajeSalida {
    @Id
    private UUID id;

    @Column(name = "solicitud_id", nullable = false, unique = true)
    private UUID solicitudId;

    @Lob
    @Column(nullable = false)
    private String contenido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoMensajeSalida estado;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_proximo_intento", nullable = false)
    private LocalDateTime fechaProximoIntento;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    protected MensajeSalida() {
    }

    public MensajeSalida(UUID id, UUID solicitudId, String contenido) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.contenido = contenido;
        this.estado = EstadoMensajeSalida.PENDIENTE;
        this.intentos = 0;
        this.fechaCreacion = LocalDateTime.now(ZoneOffset.UTC);
        this.fechaProximoIntento = fechaCreacion;
    }

    public UUID obtenerId() { return id; }
    public UUID obtenerSolicitudId() { return solicitudId; }
    public String obtenerContenido() { return contenido; }
    public EstadoMensajeSalida obtenerEstado() { return estado; }
    public int obtenerIntentos() { return intentos; }

    public void marcarPublicado() {
        if (estado != EstadoMensajeSalida.PENDIENTE) return;
        estado = EstadoMensajeSalida.PUBLICADO;
        fechaPublicacion = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void registrarFallo() {
        if (estado != EstadoMensajeSalida.PENDIENTE) return;
        intentos++;
        estado = intentos >= 5 ? EstadoMensajeSalida.FALLIDO : EstadoMensajeSalida.PENDIENTE;
        fechaProximoIntento = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(Math.min(300, 1L << intentos));
    }
}

package cl.recetaya.inventario.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "medicamento")
public class Medicamento {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(length = 100)
    private String presentacion;

    protected Medicamento() {
    }

    public Medicamento(UUID id, String codigo, String nombre, String presentacion) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.presentacion = presentacion;
    }

    public UUID obtenerId() { return id; }
    public String obtenerCodigo() { return codigo; }
    public void asignarCodigo(String codigo) { this.codigo = codigo; }
    public String obtenerNombre() { return nombre; }
    public void asignarNombre(String nombre) { this.nombre = nombre; }
    public String obtenerPresentacion() { return presentacion; }
    public void asignarPresentacion(String presentacion) { this.presentacion = presentacion; }
}

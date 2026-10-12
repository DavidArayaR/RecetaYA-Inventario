package cl.recetaya.inventario.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "sucursal")
public class Sucursal {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    protected Sucursal() {
    }

    public Sucursal(UUID id, String nombre, String direccion) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
    }

    public UUID obtenerId() { return id; }
    public String obtenerNombre() { return nombre; }
    public void asignarNombre(String nombre) { this.nombre = nombre; }
    public String obtenerDireccion() { return direccion; }
    public void asignarDireccion(String direccion) { this.direccion = direccion; }
}

package cl.recetaya.inventario.repositorio;

import cl.recetaya.inventario.entidad.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RepositorioSucursal extends JpaRepository<Sucursal, UUID> {
}

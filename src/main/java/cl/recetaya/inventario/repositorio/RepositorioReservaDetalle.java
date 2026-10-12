package cl.recetaya.inventario.repositorio;

import cl.recetaya.inventario.entidad.ClaveReservaDetalle;
import cl.recetaya.inventario.entidad.ReservaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RepositorioReservaDetalle extends JpaRepository<ReservaDetalle, ClaveReservaDetalle> {
    @Query("select d from ReservaDetalle d join fetch d.medicamento where d.reserva.id = :reservaId order by d.medicamento.id")
    List<ReservaDetalle> buscarPorReserva(@Param("reservaId") UUID reservaId);
}

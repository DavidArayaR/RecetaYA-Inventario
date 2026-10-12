package cl.recetaya.inventario.repositorio;

import cl.recetaya.inventario.entidad.MensajeSalida;
import cl.recetaya.inventario.entidad.EstadoMensajeSalida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioMensajeSalida extends JpaRepository<MensajeSalida, UUID> {
    @Query("select m from MensajeSalida m where m.solicitudId = :solicitudId")
    Optional<MensajeSalida> buscarPorSolicitud(@Param("solicitudId") UUID solicitudId);

    @Query("select m from MensajeSalida m where m.estado = :estado and m.fechaProximoIntento <= :fecha order by m.fechaCreacion")
    List<MensajeSalida> buscarPendientes(@Param("estado") EstadoMensajeSalida estado,
                                         @Param("fecha") LocalDateTime fecha, Pageable pagina);
}

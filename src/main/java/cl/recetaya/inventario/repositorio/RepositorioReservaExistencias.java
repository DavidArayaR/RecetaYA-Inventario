package cl.recetaya.inventario.repositorio;

import cl.recetaya.inventario.entidad.ReservaExistencias;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioReservaExistencias extends JpaRepository<ReservaExistencias, UUID> {
    @Query("select r from ReservaExistencias r join fetch r.sucursal where r.recetaId = :recetaId")
    Optional<ReservaExistencias> buscarPorReceta(@Param("recetaId") UUID recetaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaExistencias r join fetch r.sucursal where r.recetaId = :recetaId")
    Optional<ReservaExistencias> buscarPorRecetaParaActualizar(@Param("recetaId") UUID recetaId);
}

package cl.recetaya.inventario.repositorio;

import cl.recetaya.inventario.entidad.ExistenciaSucursal;
import cl.recetaya.inventario.entidad.ClaveExistenciaSucursal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioExistenciaSucursal extends JpaRepository<ExistenciaSucursal, ClaveExistenciaSucursal> {
    @Query("select count(s) > 0 from ExistenciaSucursal s where s.medicamento.id = :medicamentoId")
    boolean existePorMedicamento(@Param("medicamentoId") UUID medicamentoId);

    @Query("select count(s) > 0 from ExistenciaSucursal s where s.sucursal.id = :sucursalId")
    boolean existePorSucursal(@Param("sucursalId") UUID sucursalId);

    @Query("select s from ExistenciaSucursal s join fetch s.medicamento join fetch s.sucursal order by s.sucursal.nombre, s.medicamento.nombre")
    List<ExistenciaSucursal> buscarTodasConReferencias();

    @Query("select s from ExistenciaSucursal s join fetch s.medicamento join fetch s.sucursal where s.sucursal.id = :sucursalId order by s.medicamento.nombre")
    List<ExistenciaSucursal> buscarPorSucursalConReferencias(@Param("sucursalId") UUID sucursalId);

    @Query("select s from ExistenciaSucursal s join fetch s.medicamento join fetch s.sucursal where s.id = :id")
    Optional<ExistenciaSucursal> buscarPorClaveConReferencias(@Param("id") ClaveExistenciaSucursal id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ExistenciaSucursal s where s.id = :id")
    Optional<ExistenciaSucursal> buscarPorClaveParaActualizar(@Param("id") ClaveExistenciaSucursal id);
}

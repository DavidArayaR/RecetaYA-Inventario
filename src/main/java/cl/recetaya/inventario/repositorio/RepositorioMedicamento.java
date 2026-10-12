package cl.recetaya.inventario.repositorio;

import cl.recetaya.inventario.entidad.Medicamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RepositorioMedicamento extends JpaRepository<Medicamento, UUID> {
    @Query("select count(m) > 0 from Medicamento m where m.codigo = :codigo")
    boolean existePorCodigo(@Param("codigo") String codigo);

    @Query("select count(m) > 0 from Medicamento m where m.codigo = :codigo and m.id <> :id")
    boolean existePorCodigoYDistintoIdentificador(@Param("codigo") String codigo, @Param("id") UUID id);
}

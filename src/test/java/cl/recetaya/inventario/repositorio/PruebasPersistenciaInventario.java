package cl.recetaya.inventario.repositorio;

import cl.recetaya.inventario.entidad.Medicamento;
import cl.recetaya.inventario.entidad.ExistenciaSucursal;
import cl.recetaya.inventario.entidad.ClaveExistenciaSucursal;
import cl.recetaya.inventario.entidad.Sucursal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("prueba")
@Transactional
class PruebasPersistenciaInventario {
    @Autowired private Flyway flyway;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RepositorioMedicamento medicamentos;
    @Autowired private RepositorioSucursal sucursales;
    @Autowired private RepositorioExistenciaSucursal existencias;

    @Test
    void migracionInicialExisteYHibernatePuedeUsarLasEntidades() {
        assertNotNull(flyway.info().current());
        assertEquals("1", flyway.info().current().getVersion().toString());
        assertTrue(medicamentos.findAll().isEmpty());
    }

    @Test
    void persisteMedicamentoSucursalYRelacionExistencias() {
        Medicamento medicamento = medicamentos.saveAndFlush(new Medicamento(UUID.randomUUID(), "M-1", "Analgesico", null));
        Sucursal sucursal = sucursales.saveAndFlush(new Sucursal(UUID.randomUUID(), "Centro", null));
        existencias.saveAndFlush(new ExistenciaSucursal(sucursal, medicamento, 8));

        ExistenciaSucursal recuperado = existencias.buscarPorClaveConReferencias(
                new ClaveExistenciaSucursal(sucursal.obtenerId(), medicamento.obtenerId())).orElseThrow();
        assertEquals("M-1", recuperado.obtenerMedicamento().obtenerCodigo());
        assertEquals("Centro", recuperado.obtenerSucursal().obtenerNombre());
        assertEquals(8, recuperado.obtenerCantidadDisponible());
    }

    @Test
    void baseRechazaCodigosDuplicadosYExistenciasNegativas() {
        UUID medicamentoId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        jdbc.update("INSERT INTO medicamento (id, codigo, nombre) VALUES (?, ?, ?)", medicamentoId, "M-2", "Original");
        jdbc.update("INSERT INTO sucursal (id, nombre) VALUES (?, ?)", sucursalId, "Norte");

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO medicamento (id, codigo, nombre) VALUES (?, ?, ?)",
                        UUID.randomUUID(), "M-2", "Duplicado"));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO existencia_sucursal (sucursal_id, medicamento_id, cantidad_fisica, cantidad_reservada) VALUES (?, ?, ?, ?)",
                        sucursalId, medicamentoId, -1, 0));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO existencia_sucursal (sucursal_id, medicamento_id, cantidad_fisica, cantidad_reservada) VALUES (?, ?, ?, ?)",
                        sucursalId, medicamentoId, 2, 3));
    }

    @Test
    void baseRechazaExistenciasConReferenciasInexistentes() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO existencia_sucursal (sucursal_id, medicamento_id, cantidad_fisica, cantidad_reservada) VALUES (?, ?, ?, ?)",
                        UUID.randomUUID(), UUID.randomUUID(), 2, 0));
    }
}

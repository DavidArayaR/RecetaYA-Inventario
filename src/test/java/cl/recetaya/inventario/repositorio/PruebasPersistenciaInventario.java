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
        assertEquals("3", flyway.info().current().getVersion().toString());
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

    @Test
    void baseImpideReservasDuplicadasYEstadosInvalidos() {
        UUID sucursalId = UUID.randomUUID();
        UUID recetaId = UUID.randomUUID();
        jdbc.update("INSERT INTO sucursal (id, nombre) VALUES (?, ?)", sucursalId, "Centro");
        jdbc.update("INSERT INTO reserva_existencias (id, receta_id, sucursal_id, estado, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                UUID.randomUUID(), recetaId, sucursalId, "RESERVADA");

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO reserva_existencias (id, receta_id, sucursal_id, estado, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        UUID.randomUUID(), recetaId, sucursalId, "RESERVADA"));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO reserva_existencias (id, receta_id, sucursal_id, estado, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        UUID.randomUUID(), UUID.randomUUID(), sucursalId, "OTRO"));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO reserva_existencias (id, receta_id, sucursal_id, estado, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "RESERVADA"));
    }

    @Test
    void baseImpideDetallesSinReferenciaOCantidadPositiva() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO reserva_detalle (reserva_id, medicamento_id, cantidad) VALUES (?, ?, ?)",
                        UUID.randomUUID(), UUID.randomUUID(), 1));

        UUID sucursalId = UUID.randomUUID();
        UUID medicamentoId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        jdbc.update("INSERT INTO sucursal (id, nombre) VALUES (?, ?)", sucursalId, "Centro");
        jdbc.update("INSERT INTO medicamento (id, codigo, nombre) VALUES (?, ?, ?)", medicamentoId,
                "M-" + UUID.randomUUID(), "Medicamento");
        jdbc.update("INSERT INTO reserva_existencias (id, receta_id, sucursal_id, estado, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                reservaId, UUID.randomUUID(), sucursalId, "RESERVADA");
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO reserva_detalle (reserva_id, medicamento_id, cantidad) VALUES (?, ?, ?)",
                        reservaId, medicamentoId, 0));
    }
}

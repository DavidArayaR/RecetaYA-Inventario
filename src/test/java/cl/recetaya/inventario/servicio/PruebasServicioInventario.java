package cl.recetaya.inventario.servicio;

import cl.recetaya.inventario.transferencia.SolicitudMedicamento;
import cl.recetaya.inventario.transferencia.RespuestaMedicamento;
import cl.recetaya.inventario.transferencia.SolicitudSucursal;
import cl.recetaya.inventario.transferencia.RespuestaSucursal;
import cl.recetaya.inventario.excepcion.ExcepcionConflictoDatos;
import cl.recetaya.inventario.excepcion.ExcepcionRecursoNoEncontrado;
import cl.recetaya.inventario.excepcion.ExcepcionSolicitudInvalida;
import cl.recetaya.inventario.repositorio.RepositorioExistenciaSucursal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("prueba")
@Transactional
class PruebasServicioInventario {
    @Autowired private ServicioInventario servicio;
    @Autowired private RepositorioExistenciaSucursal existencias;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @Test
    void catalogoYExistenciasSeConsultanConDatosPersistidos() {
        RespuestaMedicamento medicamento = servicio.crearMedicamento(new SolicitudMedicamento("med-10", "Paracetamol", null));
        RespuestaSucursal sucursal = servicio.crearSucursal(new SolicitudSucursal("Centro", null));
        servicio.actualizarExistencias(sucursal.id(), medicamento.id(), 10);
        servicio.actualizarExistencias(sucursal.id(), medicamento.id(), 15);

        assertEquals("MED-10", servicio.consultarMedicamento(medicamento.id()).codigo());
        assertEquals(1, servicio.listarMedicamentos().size());
        assertEquals(1, servicio.listarSucursales().size());
        assertEquals(1, servicio.consultarExistenciasSucursal(sucursal.id()).size());
        assertEquals(15, servicio.consultarExistencias(sucursal.id(), medicamento.id()).cantidadDisponible());
        assertEquals(1, existencias.count());
    }

    @Test
    void reportaRecursosInexistentesYCodigoDuplicado() {
        servicio.crearMedicamento(new SolicitudMedicamento("MED-11", "Original", null));
        assertThrows(ExcepcionConflictoDatos.class,
                () -> servicio.crearMedicamento(new SolicitudMedicamento("med-11", "Duplicado", null)));
        assertThrows(ExcepcionRecursoNoEncontrado.class,
                () -> servicio.consultarMedicamento(UUID.randomUUID()));
        assertThrows(ExcepcionRecursoNoEncontrado.class,
                () -> servicio.consultarExistenciasSucursal(UUID.randomUUID()));
    }

    @Test
    void rechazaCantidadesNegativasYMenoresQueLasReservadas() {
        RespuestaMedicamento medicamento = servicio.crearMedicamento(new SolicitudMedicamento("MED-12", "Medicamento", null));
        RespuestaSucursal sucursal = servicio.crearSucursal(new SolicitudSucursal("Sur", null));
        assertThrows(ExcepcionSolicitudInvalida.class,
                () -> servicio.actualizarExistencias(sucursal.id(), medicamento.id(), -1));
        servicio.actualizarExistencias(sucursal.id(), medicamento.id(), 10);
        jdbc.update("UPDATE existencia_sucursal SET cantidad_reservada = 4 WHERE sucursal_id = ? AND medicamento_id = ?",
                sucursal.id(), medicamento.id());
        entityManager.clear();
        assertThrows(ExcepcionConflictoDatos.class,
                () -> servicio.actualizarExistencias(sucursal.id(), medicamento.id(), 3));
    }
}

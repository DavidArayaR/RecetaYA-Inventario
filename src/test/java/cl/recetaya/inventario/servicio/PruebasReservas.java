package cl.recetaya.inventario.servicio;

import cl.recetaya.inventario.entidad.EstadoReserva;
import cl.recetaya.inventario.excepcion.CodigoRechazoReserva;
import cl.recetaya.inventario.excepcion.ExcepcionReserva;
import cl.recetaya.inventario.transferencia.MedicamentoSolicitado;
import cl.recetaya.inventario.transferencia.RespuestaMedicamento;
import cl.recetaya.inventario.transferencia.RespuestaReserva;
import cl.recetaya.inventario.transferencia.RespuestaSucursal;
import cl.recetaya.inventario.transferencia.SolicitudMedicamento;
import cl.recetaya.inventario.transferencia.SolicitudReserva;
import cl.recetaya.inventario.transferencia.SolicitudSucursal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:inventario-reservas;DB_CLOSE_DELAY=-1")
@ActiveProfiles("prueba")
class PruebasReservas {
    @Autowired private ServicioInventario inventario;
    @Autowired private ServicioReservas reservas;

    @Test
    void reservaLiberaYRepiteSinDuplicarCantidades() {
        Preparacion preparacion = preparar(8);
        SolicitudReserva solicitud = solicitud(preparacion, UUID.randomUUID(), 3);

        RespuestaReserva primera = reservas.reservar(solicitud);
        RespuestaReserva repetida = reservas.reservar(solicitud);
        assertEquals(primera.id(), repetida.id());
        assertEquals(EstadoReserva.RESERVADA, primera.estado());
        assertEquals(3, inventario.consultarExistencias(preparacion.sucursalId(), preparacion.medicamentoId()).cantidadReservada());

        assertEquals(EstadoReserva.LIBERADA, reservas.liberar(solicitud.recetaId()).estado());
        assertEquals(EstadoReserva.LIBERADA, reservas.liberar(solicitud.recetaId()).estado());
        assertEquals(0, inventario.consultarExistencias(preparacion.sucursalId(), preparacion.medicamentoId()).cantidadReservada());
        assertEquals(8, inventario.consultarExistencias(preparacion.sucursalId(), preparacion.medicamentoId()).cantidadFisica());
        assertEquals(EstadoReserva.LIBERADA, reservas.reservar(solicitud).estado());
        assertEquals(CodigoRechazoReserva.ESTADO_INVALIDO,
                assertThrows(ExcepcionReserva.class, () -> reservas.consumir(solicitud.recetaId())).obtenerCodigo());
    }

    @Test
    void consumeUnaVezYNoPermiteLiberar() {
        Preparacion preparacion = preparar(8);
        SolicitudReserva solicitud = solicitud(preparacion, UUID.randomUUID(), 3);
        reservas.reservar(solicitud);

        assertEquals(EstadoReserva.CONSUMIDA, reservas.consumir(solicitud.recetaId()).estado());
        assertEquals(EstadoReserva.CONSUMIDA, reservas.consumir(solicitud.recetaId()).estado());
        assertEquals(5, inventario.consultarExistencias(preparacion.sucursalId(), preparacion.medicamentoId()).cantidadFisica());
        assertEquals(0, inventario.consultarExistencias(preparacion.sucursalId(), preparacion.medicamentoId()).cantidadReservada());
        assertEquals(CodigoRechazoReserva.ESTADO_INVALIDO,
                assertThrows(ExcepcionReserva.class, () -> reservas.liberar(solicitud.recetaId())).obtenerCodigo());
    }

    @Test
    void rechazoNoModificaExistenciasYConflictoNoDuplicaReserva() {
        Preparacion preparacion = preparar(2);
        SolicitudReserva solicitud = solicitud(preparacion, UUID.randomUUID(), 3);
        assertEquals(CodigoRechazoReserva.EXISTENCIAS_INSUFICIENTES,
                assertThrows(ExcepcionReserva.class, () -> reservas.reservar(solicitud)).obtenerCodigo());
        assertEquals(0, inventario.consultarExistencias(preparacion.sucursalId(), preparacion.medicamentoId()).cantidadReservada());

        reservas.reservar(solicitud(preparacion, solicitud.recetaId(), 1));
        assertEquals(CodigoRechazoReserva.SOLICITUD_DIFERENTE,
                assertThrows(ExcepcionReserva.class, () -> reservas.reservar(solicitud)).obtenerCodigo());
        assertEquals(1, inventario.consultarExistencias(preparacion.sucursalId(), preparacion.medicamentoId()).cantidadReservada());
    }

    @Test
    void variosMedicamentosSeReservanAtomicamente() {
        Preparacion primera = preparar(4);
        RespuestaMedicamento segundo = inventario.crearMedicamento(
                new SolicitudMedicamento("MED-" + UUID.randomUUID(), "Otro medicamento", null));
        inventario.actualizarExistencias(primera.sucursalId(), segundo.id(), 1);
        UUID recetaId = UUID.randomUUID();
        SolicitudReserva imposible = new SolicitudReserva(recetaId, primera.sucursalId(), List.of(
                new MedicamentoSolicitado(primera.medicamentoId(), 2), new MedicamentoSolicitado(segundo.id(), 2)));
        assertEquals(CodigoRechazoReserva.EXISTENCIAS_INSUFICIENTES,
                assertThrows(ExcepcionReserva.class, () -> reservas.reservar(imposible)).obtenerCodigo());
        assertEquals(0, inventario.consultarExistencias(primera.sucursalId(), primera.medicamentoId()).cantidadReservada());
        assertEquals(0, inventario.consultarExistencias(primera.sucursalId(), segundo.id()).cantidadReservada());
        assertEquals(CodigoRechazoReserva.RESERVA_INEXISTENTE,
                assertThrows(ExcepcionReserva.class, () -> reservas.consultar(recetaId)).obtenerCodigo());
    }

    @Test
    void dosRecetasConcurrentesNoSobreReservan() throws Exception {
        Preparacion preparacion = preparar(5);
        CountDownLatch inicio = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> primera = () -> intentarReserva(solicitud(preparacion, UUID.randomUUID(), 4), inicio);
            Callable<Boolean> segunda = () -> intentarReserva(solicitud(preparacion, UUID.randomUUID(), 4), inicio);
            Future<Boolean> resultadoUno = ejecutor.submit(primera);
            Future<Boolean> resultadoDos = ejecutor.submit(segunda);
            inicio.countDown();
            assertTrue(resultadoUno.get() ^ resultadoDos.get());
        }
        assertEquals(4, inventario.consultarExistencias(preparacion.sucursalId(), preparacion.medicamentoId()).cantidadReservada());
    }

    @Test
    void dosSolicitudesSimultaneasDeLaMismaRecetaReservanUnaVez() throws Exception {
        Preparacion preparacion = preparar(5);
        SolicitudReserva solicitud = solicitud(preparacion, UUID.randomUUID(), 3);
        CountDownLatch inicio = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            Callable<RespuestaReserva> tarea = () -> {
                inicio.await();
                return reservas.reservar(solicitud);
            };
            Future<RespuestaReserva> primera = ejecutor.submit(tarea);
            Future<RespuestaReserva> segunda = ejecutor.submit(tarea);
            inicio.countDown();
            assertEquals(primera.get().id(), segunda.get().id());
        }
        assertEquals(3, inventario.consultarExistencias(preparacion.sucursalId(), preparacion.medicamentoId()).cantidadReservada());
    }

    @Test
    void liberarYConsumirAlMismoTiempoSoloPermiteUnaTransicion() throws Exception {
        Preparacion preparacion = preparar(5);
        UUID recetaId = UUID.randomUUID();
        reservas.reservar(solicitud(preparacion, recetaId, 3));
        CountDownLatch inicio = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> liberar = () -> intentarTransicion(() -> reservas.liberar(recetaId), inicio);
            Callable<Boolean> consumir = () -> intentarTransicion(() -> reservas.consumir(recetaId), inicio);
            Future<Boolean> resultadoLiberacion = ejecutor.submit(liberar);
            Future<Boolean> resultadoConsumo = ejecutor.submit(consumir);
            inicio.countDown();
            assertTrue(resultadoLiberacion.get() ^ resultadoConsumo.get());
        }
        RespuestaReserva finalReserva = reservas.consultar(recetaId);
        var existencia = inventario.consultarExistencias(preparacion.sucursalId(), preparacion.medicamentoId());
        assertEquals(0, existencia.cantidadReservada());
        assertEquals(finalReserva.estado() == EstadoReserva.CONSUMIDA ? 2 : 5, existencia.cantidadFisica());
    }

    private boolean intentarTransicion(Callable<RespuestaReserva> operacion, CountDownLatch inicio) throws Exception {
        inicio.await();
        try {
            operacion.call();
            return true;
        } catch (ExcepcionReserva excepcion) {
            return false;
        }
    }

    private boolean intentarReserva(SolicitudReserva solicitud, CountDownLatch inicio) throws InterruptedException {
        inicio.await();
        try {
            reservas.reservar(solicitud);
            return true;
        } catch (ExcepcionReserva excepcion) {
            return false;
        }
    }

    private Preparacion preparar(int cantidadFisica) {
        RespuestaMedicamento medicamento = inventario.crearMedicamento(
                new SolicitudMedicamento("MED-" + UUID.randomUUID(), "Medicamento", null));
        RespuestaSucursal sucursal = inventario.crearSucursal(new SolicitudSucursal("Sucursal", null));
        inventario.actualizarExistencias(sucursal.id(), medicamento.id(), cantidadFisica);
        return new Preparacion(sucursal.id(), medicamento.id());
    }

    private SolicitudReserva solicitud(Preparacion preparacion, UUID recetaId, int cantidad) {
        return new SolicitudReserva(recetaId, preparacion.sucursalId(),
                List.of(new MedicamentoSolicitado(preparacion.medicamentoId(), cantidad)));
    }

    private record Preparacion(UUID sucursalId, UUID medicamentoId) {
    }
}

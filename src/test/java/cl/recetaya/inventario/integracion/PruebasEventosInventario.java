package cl.recetaya.inventario.integracion;

import cl.recetaya.inventario.entidad.EstadoMensajeSalida;
import cl.recetaya.inventario.entidad.MensajeSalida;
import cl.recetaya.inventario.repositorio.RepositorioMensajeSalida;
import cl.recetaya.inventario.servicio.ServicioInventario;
import cl.recetaya.inventario.transferencia.MedicamentoSolicitado;
import cl.recetaya.inventario.transferencia.RespuestaMedicamento;
import cl.recetaya.inventario.transferencia.RespuestaSucursal;
import cl.recetaya.inventario.transferencia.SolicitudMedicamento;
import cl.recetaya.inventario.transferencia.SolicitudSucursal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:inventario-eventos;DB_CLOSE_DELAY=-1")
@ActiveProfiles("prueba")
class PruebasEventosInventario {
    @Autowired private ServicioInventario inventario;
    @Autowired private ProcesadorEventosInventario procesador;
    @Autowired private RepositorioMensajeSalida salidas;
    @Autowired private ServicioBandejaSalida bandeja;
    @Autowired private ObjectMapper convertidor;

    @BeforeEach
    void limpiarBandeja() {
        salidas.deleteAll();
    }

    @Test
    void recibeReservaPublicaResultadoYRepeticionNoDuplica() {
        RespuestaMedicamento medicamento = inventario.crearMedicamento(
                new SolicitudMedicamento("MED-" + UUID.randomUUID(), "Medicamento", null));
        RespuestaSucursal sucursal = inventario.crearSucursal(new SolicitudSucursal("Sucursal", null));
        inventario.actualizarExistencias(sucursal.id(), medicamento.id(), 4);
        UUID solicitudId = UUID.randomUUID();
        UUID recetaId = UUID.randomUUID();
        EventoSolicitudInventario solicitud = new EventoSolicitudInventario(solicitudId, UUID.randomUUID(),
                TipoOperacionInventario.RESERVAR, recetaId, sucursal.id(),
                List.of(new MedicamentoSolicitado(medicamento.id(), 2)), Instant.now());
        String contenido = convertidor.writeValueAsString(solicitud);

        SqsClient cliente = mock(SqsClient.class);
        when(cliente.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder().messageId("mensaje-sqs").receiptHandle("recibo").body(contenido).build())
                .build());
        new ConsumidorSqs(cliente, procesador, "cola-solicitudes").recibir();
        verify(cliente).deleteMessage(any(software.amazon.awssdk.services.sqs.model.DeleteMessageRequest.class));
        procesador.procesar(contenido);

        assertEquals(2, inventario.consultarExistencias(sucursal.id(), medicamento.id()).cantidadReservada());
        MensajeSalida salida = salidas.buscarPorSolicitud(solicitudId).orElseThrow();
        assertTrue(salida.obtenerContenido().contains("CONFIRMADA"));
        assertEquals(1, bandeja.pendientes().stream().filter(mensaje -> mensaje.id().equals(salida.obtenerId())).count());

        when(cliente.sendMessage(any(SendMessageRequest.class))).thenReturn(
                SendMessageResponse.builder().messageId("resultado-sqs").build());
        new PublicadorSqs(cliente, bandeja, "cola-resultados").publicar();
        verify(cliente).sendMessage(any(SendMessageRequest.class));
        assertEquals(EstadoMensajeSalida.PUBLICADO, salidas.findById(salida.obtenerId()).orElseThrow().obtenerEstado());
    }

    @Test
    void rechazoPorExistenciasInsuficientesQuedaPendienteDePublicacion() {
        RespuestaMedicamento medicamento = inventario.crearMedicamento(
                new SolicitudMedicamento("MED-" + UUID.randomUUID(), "Medicamento", null));
        RespuestaSucursal sucursal = inventario.crearSucursal(new SolicitudSucursal("Sucursal", null));
        inventario.actualizarExistencias(sucursal.id(), medicamento.id(), 1);
        UUID solicitudId = UUID.randomUUID();
        EventoSolicitudInventario solicitud = new EventoSolicitudInventario(solicitudId, UUID.randomUUID(),
                TipoOperacionInventario.RESERVAR, UUID.randomUUID(), sucursal.id(),
                List.of(new MedicamentoSolicitado(medicamento.id(), 2)), Instant.now());

        procesador.procesar(convertidor.writeValueAsString(solicitud));
        MensajeSalida salida = salidas.buscarPorSolicitud(solicitudId).orElseThrow();
        assertTrue(salida.obtenerContenido().contains("EXISTENCIAS_INSUFICIENTES"));
        assertEquals(0, inventario.consultarExistencias(sucursal.id(), medicamento.id()).cantidadReservada());
        assertEquals(EstadoMensajeSalida.PENDIENTE, salida.obtenerEstado());
    }

    @Test
    void agotamientoDePublicacionMarcaFallo() {
        MensajeSalida mensaje = new MensajeSalida(UUID.randomUUID(), UUID.randomUUID(), "{}");
        for (int intento = 0; intento < 5; intento++) mensaje.registrarFallo();
        assertEquals(EstadoMensajeSalida.FALLIDO, mensaje.obtenerEstado());
        assertEquals(5, mensaje.obtenerIntentos());
    }

    @Test
    void solicitudFueraDeOrdenProduceRechazoEstable() {
        UUID solicitudId = UUID.randomUUID();
        EventoSolicitudInventario solicitud = new EventoSolicitudInventario(solicitudId, UUID.randomUUID(),
                TipoOperacionInventario.CONSUMIR, UUID.randomUUID(), null, null, Instant.now());
        procesador.procesar(convertidor.writeValueAsString(solicitud));
        assertTrue(salidas.buscarPorSolicitud(solicitudId).orElseThrow().obtenerContenido()
                .contains("RESERVA_INEXISTENTE"));
    }

    @Test
    void falloTransitorioDePublicacionConservaResultadoParaReintento() {
        UUID solicitudId = UUID.randomUUID();
        EventoSolicitudInventario solicitud = new EventoSolicitudInventario(solicitudId, UUID.randomUUID(),
                TipoOperacionInventario.LIBERAR, UUID.randomUUID(), null, null, Instant.now());
        procesador.procesar(convertidor.writeValueAsString(solicitud));
        SqsClient cliente = mock(SqsClient.class);
        when(cliente.sendMessage(any(SendMessageRequest.class))).thenThrow(new IllegalStateException("cola no disponible"));

        new PublicadorSqs(cliente, bandeja, "cola-resultados").publicar();

        MensajeSalida salida = salidas.buscarPorSolicitud(solicitudId).orElseThrow();
        assertEquals(EstadoMensajeSalida.PENDIENTE, salida.obtenerEstado());
        assertEquals(1, salida.obtenerIntentos());
    }

    @Test
    void mensajeSinIdentificadorNoSeConfirmaEnSqs() {
        SqsClient cliente = mock(SqsClient.class);
        when(cliente.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder()
                .messages(Message.builder().messageId("invalido").receiptHandle("recibo")
                        .body("{\"tipoOperacion\":\"RESERVAR\"}").build()).build());
        new ConsumidorSqs(cliente, procesador, "cola-solicitudes").recibir();
        verify(cliente, never()).deleteMessage(any(software.amazon.awssdk.services.sqs.model.DeleteMessageRequest.class));
    }
}

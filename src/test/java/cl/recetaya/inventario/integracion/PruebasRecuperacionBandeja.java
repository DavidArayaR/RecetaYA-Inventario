package cl.recetaya.inventario.integracion;

import cl.recetaya.inventario.AplicacionInventario;
import cl.recetaya.inventario.servicio.ServicioInventario;
import cl.recetaya.inventario.transferencia.MedicamentoSolicitado;
import cl.recetaya.inventario.transferencia.SolicitudMedicamento;
import cl.recetaya.inventario.transferencia.SolicitudSucursal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PruebasRecuperacionBandeja {
    @Test
    void resultadoPendienteSobreviveAlReinicio() {
        Path ruta = Path.of("target", "reserva-reinicio-" + UUID.randomUUID()).toAbsolutePath();
        String conexion = "spring.datasource.url=jdbc:h2:file:" + ruta.toString().replace('\\', '/');
        UUID solicitudId = UUID.randomUUID();

        try (ConfigurableApplicationContext primera = iniciar(conexion)) {
            ServicioInventario inventario = primera.getBean(ServicioInventario.class);
            var medicamento = inventario.crearMedicamento(
                    new SolicitudMedicamento("MED-" + UUID.randomUUID(), "Medicamento", null));
            var sucursal = inventario.crearSucursal(new SolicitudSucursal("Sucursal", null));
            inventario.actualizarExistencias(sucursal.id(), medicamento.id(), 2);
            EventoSolicitudInventario solicitud = new EventoSolicitudInventario(solicitudId, UUID.randomUUID(),
                    TipoOperacionInventario.RESERVAR, UUID.randomUUID(), sucursal.id(),
                    List.of(new MedicamentoSolicitado(medicamento.id(), 1)), Instant.now());
            primera.getBean(ProcesadorEventosInventario.class).procesar(
                    primera.getBean(ObjectMapper.class).writeValueAsString(solicitud));
        }

        try (ConfigurableApplicationContext segunda = iniciar(conexion)) {
            assertTrue(segunda.getBean(ServicioBandejaSalida.class).pendientes().stream()
                    .anyMatch(mensaje -> mensaje.contenido().contains(solicitudId.toString())));
        }
    }

    private ConfigurableApplicationContext iniciar(String conexion) {
        return new SpringApplicationBuilder(AplicacionInventario.class)
                .profiles("prueba")
                .run("--" + conexion, "--server.port=0", "--recetaya.integracion.sqs.habilitada=false");
    }
}

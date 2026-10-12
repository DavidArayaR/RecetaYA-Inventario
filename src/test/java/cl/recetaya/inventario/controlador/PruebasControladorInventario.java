package cl.recetaya.inventario.controlador;

import cl.recetaya.inventario.transferencia.SolicitudMedicamento;
import cl.recetaya.inventario.transferencia.RespuestaMedicamento;
import cl.recetaya.inventario.transferencia.SolicitudSucursal;
import cl.recetaya.inventario.transferencia.RespuestaSucursal;
import cl.recetaya.inventario.servicio.ServicioInventario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prueba")
@Transactional
class PruebasControladorInventario {
    private static final String RUTA_BASE = "/api/v1/inventario";

    @Autowired private MockMvc clienteSimulado;
    @Autowired private ServicioInventario servicio;

    @Test
    void inventarioVacioDevuelveColeccionVacia() throws Exception {
        clienteSimulado.perform(get(RUTA_BASE))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void inventarioDevuelveExistenciasPersistidasYDisponibilidad() throws Exception {
        RespuestaMedicamento medicamento = servicio.crearMedicamento(new SolicitudMedicamento("MED-1", "Paracetamol", null));
        RespuestaSucursal sucursal = servicio.crearSucursal(new SolicitudSucursal("Centro", null));
        servicio.actualizarExistencias(sucursal.id(), medicamento.id(), 12);

        clienteSimulado.perform(get(RUTA_BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medicamentoId").value(medicamento.id().toString()))
                .andExpect(jsonPath("$[0].cantidadFisica").value(12))
                .andExpect(jsonPath("$[0].cantidadReservada").value(0))
                .andExpect(jsonPath("$[0].cantidadDisponible").value(12));
    }

    @Test
    void medicamentosSePuedenCrearConsultarYActualizar() throws Exception {
        RespuestaMedicamento medicamento = servicio.crearMedicamento(new SolicitudMedicamento("MED-2", "Ibuprofeno", null));
        clienteSimulado.perform(get(RUTA_BASE + "/medicamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("MED-2"));
        clienteSimulado.perform(get(RUTA_BASE + "/medicamentos/" + medicamento.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ibuprofeno"));
        clienteSimulado.perform(put(RUTA_BASE + "/medicamentos/" + medicamento.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MED-2\",\"nombre\":\"Ibuprofeno 400\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ibuprofeno 400"));
        clienteSimulado.perform(post(RUTA_BASE + "/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MED-3\",\"nombre\":\"Amoxicilina\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.codigo").value("MED-3"));
    }

    @Test
    void sucursalesYExistenciasSeAdministranPorApi() throws Exception {
        RespuestaMedicamento medicamento = servicio.crearMedicamento(new SolicitudMedicamento("MED-4", "Vitamina C", null));
        RespuestaSucursal sucursal = servicio.crearSucursal(new SolicitudSucursal("Norte", null));
        clienteSimulado.perform(get(RUTA_BASE + "/sucursales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Norte"));
        clienteSimulado.perform(put(RUTA_BASE + "/sucursales/" + sucursal.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Norte 2\",\"direccion\":\"Calle 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Norte 2"));
        clienteSimulado.perform(put(RUTA_BASE + "/sucursales/" + sucursal.id() + "/existencias/" + medicamento.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadFisica\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(7));
        clienteSimulado.perform(get(RUTA_BASE + "/sucursales/" + sucursal.id() + "/existencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cantidadFisica").value(7));
        clienteSimulado.perform(get(RUTA_BASE + "/sucursales/" + sucursal.id() + "/existencias/" + medicamento.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreMedicamento").value("Vitamina C"));
    }

    @Test
    void validacionesYErroresDevuelvenCodigosControlados() throws Exception {
        RespuestaMedicamento medicamento = servicio.crearMedicamento(new SolicitudMedicamento("MED-5", "Aspirina", null));
        RespuestaSucursal sucursal = servicio.crearSucursal(new SolicitudSucursal("Sur", null));
        clienteSimulado.perform(post(RUTA_BASE + "/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"MED-5\",\"nombre\":\"Duplicado\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigoEstado").value(409));
        clienteSimulado.perform(post(RUTA_BASE + "/medicamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigoEstado").value(400));
        clienteSimulado.perform(get(RUTA_BASE + "/medicamentos/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigoEstado").value(404));
        clienteSimulado.perform(put(RUTA_BASE + "/sucursales/" + sucursal.id() + "/existencias/" + medicamento.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadFisica\":-1}"))
                .andExpect(status().isBadRequest());
        clienteSimulado.perform(put(RUTA_BASE + "/sucursales/" + sucursal.id() + "/existencias/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadFisica\":1}"))
                .andExpect(status().isNotFound());
    }
}

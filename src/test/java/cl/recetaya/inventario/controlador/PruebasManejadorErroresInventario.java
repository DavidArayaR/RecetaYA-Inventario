package cl.recetaya.inventario.controlador;

import cl.recetaya.inventario.servicio.ServicioInventario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prueba")
class PruebasManejadorErroresInventario {
    @Autowired private MockMvc clienteSimulado;
    @MockitoBean private ServicioInventario servicio;

    @Test
    void errorInesperadoNoExponeDetallesInternos() throws Exception {
        when(servicio.consultarInventario()).thenThrow(new IllegalStateException("detalle interno sensible"));

        clienteSimulado.perform(get("/api/v1/inventario"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigoEstado").value(500))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error interno al procesar la solicitud."));
    }
}

package cl.recetaya.inventario.controller;

import cl.recetaya.inventario.exception.FuncionalidadNoImplementadaException;
import cl.recetaya.inventario.service.InventarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventarioControllerTests {

    private static final String ENDPOINT = "/api/v1/inventario";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventarioService inventarioService;

    @Test
    void getInventarioRespondeNoImplementadoConFormatoJsonYDelegaAlServicio() throws Exception {
        doThrow(new FuncionalidadNoImplementadaException(
                "La consulta de inventario todavía no está implementada."))
                .when(inventarioService).consultarInventario();

        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isNotImplemented())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(501))
                .andExpect(jsonPath("$.error").value("Not Implemented"))
                .andExpect(jsonPath("$.message").value(
                        "La consulta de inventario todavía no está implementada."))
                .andExpect(jsonPath("$.path").value(ENDPOINT));

        verify(inventarioService).consultarInventario();
    }

    @Test
    void errorInesperadoDevuelveRespuestaGenericaSinExponerDetallesInternos() throws Exception {
        doThrow(new IllegalStateException("detalle interno sensible"))
                .when(inventarioService).consultarInventario();

        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(
                        "Ocurrió un error interno al procesar la solicitud."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("detalle interno sensible"))));
    }
}
package cl.recetaya.inventario.integracion;

import cl.recetaya.inventario.excepcion.CodigoRechazoReserva;
import cl.recetaya.inventario.excepcion.ExcepcionReserva;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CodificadorEventos {
    private final ObjectMapper convertidor;

    public CodificadorEventos(ObjectMapper convertidor) {
        this.convertidor = convertidor;
    }

    public EventoSolicitudInventario leerSolicitud(String contenido) {
        try {
            return convertidor.readValue(contenido, EventoSolicitudInventario.class);
        } catch (Exception excepcion) {
            throw new ExcepcionReserva(CodigoRechazoReserva.SOLICITUD_INVALIDA,
                    "El mensaje de solicitud no es JSON válido.");
        }
    }

    public String escribirResultado(EventoResultadoInventario resultado) {
        return convertidor.writeValueAsString(resultado);
    }
}

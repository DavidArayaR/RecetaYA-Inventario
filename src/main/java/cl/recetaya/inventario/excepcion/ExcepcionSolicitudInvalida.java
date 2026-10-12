package cl.recetaya.inventario.excepcion;

public class ExcepcionSolicitudInvalida extends RuntimeException {
    public ExcepcionSolicitudInvalida(String mensaje) {
        super(mensaje);
    }
}

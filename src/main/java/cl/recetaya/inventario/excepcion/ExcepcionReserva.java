package cl.recetaya.inventario.excepcion;

public class ExcepcionReserva extends RuntimeException {
    private final CodigoRechazoReserva codigo;

    public ExcepcionReserva(CodigoRechazoReserva codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public CodigoRechazoReserva obtenerCodigo() { return codigo; }
}

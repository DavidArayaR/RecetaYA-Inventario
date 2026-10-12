package cl.recetaya.inventario.excepcion;

public class ExcepcionConflictoDatos extends RuntimeException {
    public ExcepcionConflictoDatos(String mensaje) {
        super(mensaje);
    }
}

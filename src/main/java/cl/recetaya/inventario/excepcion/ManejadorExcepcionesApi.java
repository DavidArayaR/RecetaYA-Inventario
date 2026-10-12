package cl.recetaya.inventario.excepcion;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class ManejadorExcepcionesApi {

    @ExceptionHandler(ExcepcionRecursoNoEncontrado.class)
    public ResponseEntity<RespuestaErrorApi> manejarNoEncontrado(
            ExcepcionRecursoNoEncontrado excepcion,
            HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.NOT_FOUND, excepcion.getMessage(), solicitud);
    }

    @ExceptionHandler(ExcepcionConflictoDatos.class)
    public ResponseEntity<RespuestaErrorApi> manejarConflicto(
            ExcepcionConflictoDatos excepcion, HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.CONFLICT, excepcion.getMessage(), solicitud);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaErrorApi> manejarIntegridad(HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.CONFLICT, "Los datos entran en conflicto con una restricción de integridad.", solicitud);
    }

    @ExceptionHandler({ExcepcionSolicitudInvalida.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class})
    public ResponseEntity<RespuestaErrorApi> manejarSolicitudInvalida(
            Exception excepcion,
            HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos.", solicitud);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaErrorApi> manejarErrorInesperado(
            Exception excepcion,
            HttpServletRequest solicitud) {
        return crearRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno al procesar la solicitud.",
                solicitud);
    }

    private ResponseEntity<RespuestaErrorApi> crearRespuesta(
            HttpStatus estadoHttp,
            String mensaje,
            HttpServletRequest solicitud) {
        RespuestaErrorApi cuerpo = new RespuestaErrorApi(
                Instant.now(),
                estadoHttp.value(),
                descripcionEstado(estadoHttp),
                mensaje,
                solicitud.getRequestURI());
        return ResponseEntity.status(estadoHttp).body(cuerpo);
    }

    private String descripcionEstado(HttpStatus estadoHttp) {
        return switch (estadoHttp) {
            case BAD_REQUEST -> "Solicitud incorrecta";
            case NOT_FOUND -> "Recurso no encontrado";
            case CONFLICT -> "Conflicto";
            case INTERNAL_SERVER_ERROR -> "Error interno del servidor";
            default -> "Error";
        };
    }
}

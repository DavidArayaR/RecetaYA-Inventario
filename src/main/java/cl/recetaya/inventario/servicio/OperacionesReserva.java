package cl.recetaya.inventario.servicio;

import cl.recetaya.inventario.entidad.ClaveExistenciaSucursal;
import cl.recetaya.inventario.entidad.EstadoReserva;
import cl.recetaya.inventario.entidad.ExistenciaSucursal;
import cl.recetaya.inventario.entidad.Medicamento;
import cl.recetaya.inventario.entidad.ReservaDetalle;
import cl.recetaya.inventario.entidad.ReservaExistencias;
import cl.recetaya.inventario.entidad.Sucursal;
import cl.recetaya.inventario.excepcion.CodigoRechazoReserva;
import cl.recetaya.inventario.excepcion.ExcepcionReserva;
import cl.recetaya.inventario.repositorio.RepositorioExistenciaSucursal;
import cl.recetaya.inventario.repositorio.RepositorioMedicamento;
import cl.recetaya.inventario.repositorio.RepositorioReservaDetalle;
import cl.recetaya.inventario.repositorio.RepositorioReservaExistencias;
import cl.recetaya.inventario.repositorio.RepositorioSucursal;
import cl.recetaya.inventario.transferencia.DetalleReserva;
import cl.recetaya.inventario.transferencia.MedicamentoSolicitado;
import cl.recetaya.inventario.transferencia.RespuestaReserva;
import cl.recetaya.inventario.transferencia.SolicitudReserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class OperacionesReserva {
    private final RepositorioReservaExistencias reservas;
    private final RepositorioReservaDetalle detalles;
    private final RepositorioExistenciaSucursal existencias;
    private final RepositorioMedicamento medicamentos;
    private final RepositorioSucursal sucursales;

    public OperacionesReserva(RepositorioReservaExistencias reservas, RepositorioReservaDetalle detalles,
                             RepositorioExistenciaSucursal existencias, RepositorioMedicamento medicamentos,
                             RepositorioSucursal sucursales) {
        this.reservas = reservas;
        this.detalles = detalles;
        this.existencias = existencias;
        this.medicamentos = medicamentos;
        this.sucursales = sucursales;
    }

    @Transactional
    public RespuestaReserva reservar(SolicitudReserva solicitud) {
        Map<UUID, Integer> cantidades = validarYOrdenar(solicitud);
        ReservaExistencias anterior = reservas.buscarPorRecetaParaActualizar(solicitud.recetaId()).orElse(null);
        if (anterior != null) return comprobarReintento(anterior, solicitud.sucursalId(), cantidades);

        Sucursal sucursal = sucursales.findById(solicitud.sucursalId())
                .orElseThrow(() -> rechazo(CodigoRechazoReserva.SUCURSAL_INEXISTENTE, "Sucursal no encontrada."));
        ReservaExistencias reserva = reservas.saveAndFlush(
                new ReservaExistencias(UUID.randomUUID(), solicitud.recetaId(), sucursal));

        for (Map.Entry<UUID, Integer> linea : cantidades.entrySet()) {
            Medicamento medicamento = medicamentos.findById(linea.getKey())
                    .orElseThrow(() -> rechazo(CodigoRechazoReserva.MEDICAMENTO_INEXISTENTE,
                            "Medicamento no encontrado."));
            ExistenciaSucursal existencia = bloquearExistencia(sucursal.obtenerId(), linea.getKey());
            if (existencia.obtenerCantidadDisponible() < linea.getValue()) {
                throw rechazo(CodigoRechazoReserva.EXISTENCIAS_INSUFICIENTES,
                        "Existencias insuficientes para el medicamento " + linea.getKey() + ".");
            }
            existencia.reservar(linea.getValue());
            detalles.save(new ReservaDetalle(reserva, medicamento, linea.getValue()));
        }
        detalles.flush();
        existencias.flush();
        return responder(reserva);
    }

    @Transactional
    public RespuestaReserva liberar(UUID recetaId) {
        ReservaExistencias reserva = bloquearReserva(recetaId);
        if (reserva.obtenerEstado() == EstadoReserva.LIBERADA) return responder(reserva);
        exigirReservaActiva(reserva);
        for (ReservaDetalle detalle : detalles.buscarPorReserva(reserva.obtenerId())) {
            ExistenciaSucursal existencia = bloquearExistencia(
                    reserva.obtenerSucursal().obtenerId(), detalle.obtenerMedicamento().obtenerId());
            existencia.liberar(detalle.obtenerCantidad());
        }
        reserva.cambiarEstado(EstadoReserva.LIBERADA);
        existencias.flush();
        return responder(reserva);
    }

    @Transactional
    public RespuestaReserva consumir(UUID recetaId) {
        ReservaExistencias reserva = bloquearReserva(recetaId);
        if (reserva.obtenerEstado() == EstadoReserva.CONSUMIDA) return responder(reserva);
        exigirReservaActiva(reserva);
        for (ReservaDetalle detalle : detalles.buscarPorReserva(reserva.obtenerId())) {
            ExistenciaSucursal existencia = bloquearExistencia(
                    reserva.obtenerSucursal().obtenerId(), detalle.obtenerMedicamento().obtenerId());
            existencia.consumir(detalle.obtenerCantidad());
        }
        reserva.cambiarEstado(EstadoReserva.CONSUMIDA);
        existencias.flush();
        return responder(reserva);
    }

    @Transactional(readOnly = true)
    public RespuestaReserva consultar(UUID recetaId) {
        ReservaExistencias reserva = reservas.buscarPorReceta(recetaId)
                .orElseThrow(() -> rechazo(CodigoRechazoReserva.RESERVA_INEXISTENTE, "Reserva no encontrada."));
        return responder(reserva);
    }

    @Transactional(readOnly = true)
    public RespuestaReserva consultarReintento(SolicitudReserva solicitud) {
        Map<UUID, Integer> cantidades = validarYOrdenar(solicitud);
        ReservaExistencias reserva = reservas.buscarPorReceta(solicitud.recetaId())
                .orElseThrow(() -> rechazo(CodigoRechazoReserva.CONFLICTO_CONCURRENTE,
                        "No se pudo resolver la solicitud concurrente."));
        return comprobarReintento(reserva, solicitud.sucursalId(), cantidades);
    }

    private ReservaExistencias bloquearReserva(UUID recetaId) {
        if (recetaId == null) throw rechazo(CodigoRechazoReserva.SOLICITUD_INVALIDA, "La receta es obligatoria.");
        return reservas.buscarPorRecetaParaActualizar(recetaId)
                .orElseThrow(() -> rechazo(CodigoRechazoReserva.RESERVA_INEXISTENTE, "Reserva no encontrada."));
    }

    private ExistenciaSucursal bloquearExistencia(UUID sucursalId, UUID medicamentoId) {
        return existencias.buscarPorClaveParaActualizar(new ClaveExistenciaSucursal(sucursalId, medicamentoId))
                .orElseThrow(() -> rechazo(CodigoRechazoReserva.EXISTENCIA_INEXISTENTE,
                        "No hay existencias para el medicamento " + medicamentoId + " en la sucursal."));
    }

    private void exigirReservaActiva(ReservaExistencias reserva) {
        if (reserva.obtenerEstado() != EstadoReserva.RESERVADA) {
            throw rechazo(CodigoRechazoReserva.ESTADO_INVALIDO,
                    "La reserva no está activa para esta operación.");
        }
    }

    private Map<UUID, Integer> validarYOrdenar(SolicitudReserva solicitud) {
        if (solicitud == null || solicitud.recetaId() == null || solicitud.sucursalId() == null
                || solicitud.medicamentos() == null || solicitud.medicamentos().isEmpty()) {
            throw rechazo(CodigoRechazoReserva.SOLICITUD_INVALIDA, "La reserva requiere receta, sucursal y medicamentos.");
        }
        Map<UUID, Integer> cantidades = new TreeMap<>();
        for (MedicamentoSolicitado linea : solicitud.medicamentos()) {
            if (linea == null || linea.medicamentoId() == null || linea.cantidad() <= 0
                    || cantidades.putIfAbsent(linea.medicamentoId(), linea.cantidad()) != null) {
                throw rechazo(CodigoRechazoReserva.SOLICITUD_INVALIDA,
                        "Los medicamentos deben ser únicos y tener cantidades positivas.");
            }
        }
        return cantidades;
    }

    private RespuestaReserva comprobarReintento(ReservaExistencias reserva, UUID sucursalId,
                                                 Map<UUID, Integer> cantidades) {
        RespuestaReserva respuesta = responder(reserva);
        Map<UUID, Integer> anteriores = new TreeMap<>();
        respuesta.medicamentos().forEach(linea -> anteriores.put(linea.medicamentoId(), linea.cantidad()));
        if (!respuesta.sucursalId().equals(sucursalId) || !anteriores.equals(cantidades)) {
            throw rechazo(CodigoRechazoReserva.SOLICITUD_DIFERENTE,
                    "La receta ya tiene una reserva con datos diferentes.");
        }
        return respuesta;
    }

    private RespuestaReserva responder(ReservaExistencias reserva) {
        List<DetalleReserva> lineas = detalles.buscarPorReserva(reserva.obtenerId()).stream()
                .map(detalle -> new DetalleReserva(detalle.obtenerMedicamento().obtenerId(), detalle.obtenerCantidad()))
                .toList();
        return new RespuestaReserva(reserva.obtenerId(), reserva.obtenerRecetaId(),
                reserva.obtenerSucursal().obtenerId(), reserva.obtenerEstado(), lineas,
                reserva.obtenerFechaCreacion().toInstant(ZoneOffset.UTC),
                reserva.obtenerFechaActualizacion().toInstant(ZoneOffset.UTC));
    }

    private ExcepcionReserva rechazo(CodigoRechazoReserva codigo, String mensaje) {
        return new ExcepcionReserva(codigo, mensaje);
    }
}

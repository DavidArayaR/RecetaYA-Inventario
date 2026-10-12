package cl.recetaya.inventario.servicio;

import cl.recetaya.inventario.transferencia.SolicitudMedicamento;
import cl.recetaya.inventario.transferencia.RespuestaMedicamento;
import cl.recetaya.inventario.transferencia.RespuestaExistencias;
import cl.recetaya.inventario.transferencia.SolicitudSucursal;
import cl.recetaya.inventario.transferencia.RespuestaSucursal;
import cl.recetaya.inventario.entidad.Medicamento;
import cl.recetaya.inventario.entidad.ExistenciaSucursal;
import cl.recetaya.inventario.entidad.ClaveExistenciaSucursal;
import cl.recetaya.inventario.entidad.Sucursal;
import cl.recetaya.inventario.excepcion.ExcepcionConflictoDatos;
import cl.recetaya.inventario.excepcion.ExcepcionRecursoNoEncontrado;
import cl.recetaya.inventario.excepcion.ExcepcionSolicitudInvalida;
import cl.recetaya.inventario.repositorio.RepositorioMedicamento;
import cl.recetaya.inventario.repositorio.RepositorioExistenciaSucursal;
import cl.recetaya.inventario.repositorio.RepositorioSucursal;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ServicioInventario {
    private final RepositorioMedicamento medicamentos;
    private final RepositorioSucursal sucursales;
    private final RepositorioExistenciaSucursal existencias;

    public ServicioInventario(RepositorioMedicamento medicamentos,
                             RepositorioSucursal sucursales,
                             RepositorioExistenciaSucursal existencias) {
        this.medicamentos = medicamentos;
        this.sucursales = sucursales;
        this.existencias = existencias;
    }

    public List<RespuestaExistencias> consultarInventario() {
        return existencias.buscarTodasConReferencias().stream().map(this::aRespuestaExistencias).toList();
    }

    public List<RespuestaMedicamento> listarMedicamentos() {
        return medicamentos.findAll(Sort.by("nombre")).stream().map(this::aRespuestaMedicamento).toList();
    }

    public RespuestaMedicamento consultarMedicamento(UUID id) {
        return aRespuestaMedicamento(buscarMedicamento(id));
    }

    @Transactional
    public RespuestaMedicamento crearMedicamento(SolicitudMedicamento solicitud) {
        String codigo = normalizarCodigo(solicitud.codigo());
        if (medicamentos.existePorCodigo(codigo)) {
            throw new ExcepcionConflictoDatos("Ya existe un medicamento con ese código.");
        }
        Medicamento medicamento = new Medicamento(UUID.randomUUID(), codigo,
                solicitud.nombre().trim(), limpiarOpcional(solicitud.presentacion()));
        return aRespuestaMedicamento(medicamentos.saveAndFlush(medicamento));
    }

    @Transactional
    public RespuestaMedicamento actualizarMedicamento(UUID id, SolicitudMedicamento solicitud) {
        Medicamento medicamento = buscarMedicamento(id);
        String codigo = normalizarCodigo(solicitud.codigo());
        if (medicamentos.existePorCodigoYDistintoIdentificador(codigo, id)) {
            throw new ExcepcionConflictoDatos("Ya existe un medicamento con ese código.");
        }
        medicamento.asignarCodigo(codigo);
        medicamento.asignarNombre(solicitud.nombre().trim());
        medicamento.asignarPresentacion(limpiarOpcional(solicitud.presentacion()));
        return aRespuestaMedicamento(medicamentos.saveAndFlush(medicamento));
    }

    public List<RespuestaSucursal> listarSucursales() {
        return sucursales.findAll(Sort.by("nombre")).stream().map(this::aRespuestaSucursal).toList();
    }

    public RespuestaSucursal consultarSucursal(UUID id) {
        return aRespuestaSucursal(buscarSucursal(id));
    }

    @Transactional
    public RespuestaSucursal crearSucursal(SolicitudSucursal solicitud) {
        Sucursal sucursal = new Sucursal(UUID.randomUUID(), solicitud.nombre().trim(),
                limpiarOpcional(solicitud.direccion()));
        return aRespuestaSucursal(sucursales.saveAndFlush(sucursal));
    }

    @Transactional
    public RespuestaSucursal actualizarSucursal(UUID id, SolicitudSucursal solicitud) {
        Sucursal sucursal = buscarSucursal(id);
        sucursal.asignarNombre(solicitud.nombre().trim());
        sucursal.asignarDireccion(limpiarOpcional(solicitud.direccion()));
        return aRespuestaSucursal(sucursales.saveAndFlush(sucursal));
    }

    public List<RespuestaExistencias> consultarExistenciasSucursal(UUID sucursalId) {
        buscarSucursal(sucursalId);
        return existencias.buscarPorSucursalConReferencias(sucursalId).stream().map(this::aRespuestaExistencias).toList();
    }

    public RespuestaExistencias consultarExistencias(UUID sucursalId, UUID medicamentoId) {
        return existencias.buscarPorClaveConReferencias(new ClaveExistenciaSucursal(sucursalId, medicamentoId))
                .map(this::aRespuestaExistencias)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No existe existencia para ese medicamento en la sucursal."));
    }

    @Transactional
    public RespuestaExistencias actualizarExistencias(UUID sucursalId, UUID medicamentoId, int cantidadFisica) {
        if (cantidadFisica < 0) {
            throw new ExcepcionSolicitudInvalida("La cantidad física no puede ser negativa.");
        }
        Sucursal sucursal = buscarSucursal(sucursalId);
        Medicamento medicamento = buscarMedicamento(medicamentoId);
        ClaveExistenciaSucursal id = new ClaveExistenciaSucursal(sucursalId, medicamentoId);
        ExistenciaSucursal existencia = existencias.buscarPorClaveParaActualizar(id)
                .orElseGet(() -> new ExistenciaSucursal(sucursal, medicamento, cantidadFisica));
        if (cantidadFisica < existencia.obtenerCantidadReservada()) {
            throw new ExcepcionConflictoDatos("La cantidad física no puede ser menor que la reservada.");
        }
        existencia.asignarCantidadFisica(cantidadFisica);
        return aRespuestaExistencias(existencias.saveAndFlush(existencia));
    }

    private Medicamento buscarMedicamento(UUID id) {
        return medicamentos.findById(id)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Medicamento no encontrado."));
    }

    private Sucursal buscarSucursal(UUID id) {
        return sucursales.findById(id)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Sucursal no encontrada."));
    }

    private RespuestaMedicamento aRespuestaMedicamento(Medicamento medicamento) {
        return new RespuestaMedicamento(medicamento.obtenerId(), medicamento.obtenerCodigo(),
                medicamento.obtenerNombre(), medicamento.obtenerPresentacion());
    }

    private RespuestaSucursal aRespuestaSucursal(Sucursal sucursal) {
        return new RespuestaSucursal(sucursal.obtenerId(), sucursal.obtenerNombre(), sucursal.obtenerDireccion());
    }

    private RespuestaExistencias aRespuestaExistencias(ExistenciaSucursal existencia) {
        return new RespuestaExistencias(existencia.obtenerSucursal().obtenerId(), existencia.obtenerMedicamento().obtenerId(),
                existencia.obtenerMedicamento().obtenerNombre(), existencia.obtenerCantidadFisica(),
                existencia.obtenerCantidadReservada(), existencia.obtenerCantidadDisponible());
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String limpiarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}

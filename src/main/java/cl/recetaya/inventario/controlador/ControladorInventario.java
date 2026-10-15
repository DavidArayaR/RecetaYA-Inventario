package cl.recetaya.inventario.controlador;

import cl.recetaya.inventario.transferencia.SolicitudMedicamento;
import cl.recetaya.inventario.transferencia.RespuestaMedicamento;
import cl.recetaya.inventario.transferencia.RespuestaExistencias;
import cl.recetaya.inventario.transferencia.SolicitudActualizacionExistencias;
import cl.recetaya.inventario.transferencia.SolicitudSucursal;
import cl.recetaya.inventario.transferencia.RespuestaSucursal;
import cl.recetaya.inventario.servicio.ServicioInventario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventario")
public class ControladorInventario {

    private final ServicioInventario servicioInventario;

    public ControladorInventario(ServicioInventario servicioInventario) {
        this.servicioInventario = servicioInventario;
    }

    @GetMapping
    public List<RespuestaExistencias> consultarInventario() {
        return servicioInventario.consultarInventario();
    }

    @GetMapping("/medicamentos")
    public List<RespuestaMedicamento> listarMedicamentos() {
        return servicioInventario.listarMedicamentos();
    }

    @GetMapping("/medicamentos/{id}")
    public RespuestaMedicamento consultarMedicamento(@PathVariable UUID id) {
        return servicioInventario.consultarMedicamento(id);
    }

    @PostMapping("/medicamentos")
    public ResponseEntity<RespuestaMedicamento> crearMedicamento(@Valid @RequestBody SolicitudMedicamento solicitud) {
        RespuestaMedicamento creado = servicioInventario.crearMedicamento(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/inventario/medicamentos/" + creado.id())).body(creado);
    }

    @PutMapping("/medicamentos/{id}")
    public RespuestaMedicamento actualizarMedicamento(@PathVariable UUID id,
                                                      @Valid @RequestBody SolicitudMedicamento solicitud) {
        return servicioInventario.actualizarMedicamento(id, solicitud);
    }

    @DeleteMapping("/medicamentos/{id}")
    public ResponseEntity<Void> eliminarMedicamento(@PathVariable UUID id) {
        servicioInventario.eliminarMedicamento(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sucursales")
    public List<RespuestaSucursal> listarSucursales() {
        return servicioInventario.listarSucursales();
    }

    @GetMapping("/sucursales/{id}")
    public RespuestaSucursal consultarSucursal(@PathVariable UUID id) {
        return servicioInventario.consultarSucursal(id);
    }

    @PostMapping("/sucursales")
    public ResponseEntity<RespuestaSucursal> crearSucursal(@Valid @RequestBody SolicitudSucursal solicitud) {
        RespuestaSucursal creada = servicioInventario.crearSucursal(solicitud);
        return ResponseEntity.created(URI.create("/api/v1/inventario/sucursales/" + creada.id())).body(creada);
    }

    @PutMapping("/sucursales/{id}")
    public RespuestaSucursal actualizarSucursal(@PathVariable UUID id,
                                               @Valid @RequestBody SolicitudSucursal solicitud) {
        return servicioInventario.actualizarSucursal(id, solicitud);
    }

    @DeleteMapping("/sucursales/{id}")
    public ResponseEntity<Void> eliminarSucursal(@PathVariable UUID id) {
        servicioInventario.eliminarSucursal(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sucursales/{id}/existencias")
    public List<RespuestaExistencias> consultarExistenciasSucursal(@PathVariable UUID id) {
        return servicioInventario.consultarExistenciasSucursal(id);
    }

    @GetMapping("/sucursales/{id}/existencias/{medicamentoId}")
    public RespuestaExistencias consultarExistencias(@PathVariable UUID id, @PathVariable UUID medicamentoId) {
        return servicioInventario.consultarExistencias(id, medicamentoId);
    }

    @PutMapping("/sucursales/{id}/existencias/{medicamentoId}")
    public RespuestaExistencias actualizarExistencias(@PathVariable UUID id,
                                        @PathVariable UUID medicamentoId,
                                        @Valid @RequestBody SolicitudActualizacionExistencias solicitud) {
        return servicioInventario.actualizarExistencias(id, medicamentoId, solicitud.cantidadFisica());
    }
}

package cl.recetaya.inventario.integracion;

import cl.recetaya.inventario.entidad.EstadoMensajeSalida;
import cl.recetaya.inventario.entidad.MensajeSalida;
import cl.recetaya.inventario.repositorio.RepositorioMensajeSalida;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class ServicioBandejaSalida {
    private final RepositorioMensajeSalida mensajes;

    public ServicioBandejaSalida(RepositorioMensajeSalida mensajes) {
        this.mensajes = mensajes;
    }

    @Transactional(readOnly = true)
    public List<MensajePendiente> pendientes() {
        return mensajes.buscarPendientes(EstadoMensajeSalida.PENDIENTE,
                        LocalDateTime.now(ZoneOffset.UTC), PageRequest.of(0, 10)).stream()
                .map(mensaje -> new MensajePendiente(mensaje.obtenerId(), mensaje.obtenerContenido()))
                .toList();
    }

    @Transactional
    public void marcarPublicado(UUID id) {
        MensajeSalida mensaje = mensajes.findById(id).orElseThrow();
        mensaje.marcarPublicado();
    }

    @Transactional
    public void registrarFallo(UUID id) {
        MensajeSalida mensaje = mensajes.findById(id).orElseThrow();
        mensaje.registrarFallo();
    }
}

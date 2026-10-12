package cl.recetaya.inventario.service;

import cl.recetaya.inventario.exception.FuncionalidadNoImplementadaException;
import org.springframework.stereotype.Service;

@Service
public class InventarioService {

    public void consultarInventario() {
        throw new FuncionalidadNoImplementadaException(
                "La consulta de inventario todavía no está implementada.");
    }
}
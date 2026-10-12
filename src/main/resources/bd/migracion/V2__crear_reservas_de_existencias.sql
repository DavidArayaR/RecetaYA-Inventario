CREATE TABLE reserva_existencias (
    id UUID NOT NULL PRIMARY KEY,
    receta_id UUID NOT NULL,
    sucursal_id UUID NOT NULL,
    estado VARCHAR(24) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    CONSTRAINT unico_reserva_receta UNIQUE (receta_id),
    CONSTRAINT referencia_reserva_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal (id),
    CONSTRAINT validacion_estado_reserva CHECK (estado IN ('PENDIENTE', 'RESERVADA', 'RECHAZADA', 'LIBERADA', 'CONSUMIDA'))
);

CREATE TABLE reserva_detalle (
    reserva_id UUID NOT NULL,
    medicamento_id UUID NOT NULL,
    cantidad INTEGER NOT NULL,
    CONSTRAINT clave_primaria_reserva_detalle PRIMARY KEY (reserva_id, medicamento_id),
    CONSTRAINT referencia_detalle_reserva FOREIGN KEY (reserva_id) REFERENCES reserva_existencias (id),
    CONSTRAINT referencia_detalle_medicamento FOREIGN KEY (medicamento_id) REFERENCES medicamento (id),
    CONSTRAINT validacion_cantidad_reserva CHECK (cantidad > 0)
);

CREATE INDEX indice_reserva_sucursal_estado ON reserva_existencias (sucursal_id, estado);
CREATE INDEX indice_detalle_medicamento ON reserva_detalle (medicamento_id);

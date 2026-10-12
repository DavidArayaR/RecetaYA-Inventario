CREATE TABLE mensaje_salida (
    id UUID NOT NULL PRIMARY KEY,
    solicitud_id UUID NOT NULL,
    contenido CLOB NOT NULL,
    estado VARCHAR(16) NOT NULL,
    intentos INTEGER NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_proximo_intento TIMESTAMP NOT NULL,
    fecha_publicacion TIMESTAMP,
    CONSTRAINT unico_mensaje_solicitud UNIQUE (solicitud_id),
    CONSTRAINT validacion_estado_mensaje CHECK (estado IN ('PENDIENTE', 'PUBLICADO', 'FALLIDO')),
    CONSTRAINT validacion_intentos_mensaje CHECK (intentos >= 0)
);

CREATE INDEX indice_mensaje_pendiente ON mensaje_salida (estado, fecha_proximo_intento);

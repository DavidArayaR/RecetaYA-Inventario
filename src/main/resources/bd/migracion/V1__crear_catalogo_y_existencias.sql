CREATE TABLE medicamento (
    id UUID NOT NULL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(160) NOT NULL,
    presentacion VARCHAR(100),
    CONSTRAINT unico_medicamento_codigo UNIQUE (codigo)
);

CREATE TABLE sucursal (
    id UUID NOT NULL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    direccion VARCHAR(255)
);

CREATE TABLE existencia_sucursal (
    sucursal_id UUID NOT NULL,
    medicamento_id UUID NOT NULL,
    cantidad_fisica INTEGER NOT NULL,
    cantidad_reservada INTEGER NOT NULL,
    CONSTRAINT clave_primaria_existencia_sucursal PRIMARY KEY (sucursal_id, medicamento_id),
    CONSTRAINT referencia_existencia_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal (id),
    CONSTRAINT referencia_existencia_medicamento FOREIGN KEY (medicamento_id) REFERENCES medicamento (id),
    CONSTRAINT validacion_cantidad_fisica CHECK (cantidad_fisica >= 0),
    CONSTRAINT validacion_cantidad_reservada CHECK (cantidad_reservada >= 0),
    CONSTRAINT validacion_cantidad_disponible CHECK (cantidad_reservada <= cantidad_fisica)
);

CREATE INDEX indice_existencia_medicamento ON existencia_sucursal (medicamento_id);

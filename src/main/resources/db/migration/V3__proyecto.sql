-- Proyectos (etiquetas): agrupan tareas, por ejemplo "Tesis", "Facultad" o "Personal" (docs/01-especificacion.md §4.6).
CREATE TABLE proyecto (
    id          UUID         PRIMARY KEY,
    usuario_id  UUID         NOT NULL,
    nombre      VARCHAR(40)  NOT NULL,
    color       VARCHAR(10)  NOT NULL
                CHECK (color IN ('TERRACOTA', 'OCRE', 'OLIVA', 'PINO', 'AZUL', 'CIRUELA', 'PIZARRA')),
    creado_en   TIMESTAMPTZ  NOT NULL
);

-- Un nombre por usuario, sin distinguir mayúsculas
CREATE UNIQUE INDEX proyecto_usuario_nombre_idx ON proyecto (usuario_id, lower(nombre));

-- Al eliminar un proyecto, sus tareas quedan sin proyecto
ALTER TABLE tarea ADD COLUMN proyecto_id UUID REFERENCES proyecto (id) ON DELETE SET NULL;
CREATE INDEX tarea_proyecto_idx ON tarea (proyecto_id) WHERE proyecto_id IS NOT NULL;

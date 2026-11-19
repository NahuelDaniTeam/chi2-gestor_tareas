-- Tareas del gestor (docs/01-especificacion.md §7).
-- Etapa 2 suma las tablas grupo y miembro_grupo y la FK de grupo_id / asignado_a.
-- Nunca editar una migración ya mergeada a main: crear una nueva.
CREATE TABLE tarea (
    id                UUID          PRIMARY KEY,
    titulo            VARCHAR(200)  NOT NULL,
    descripcion       VARCHAR(2000),
    horizonte         VARCHAR(12)   NOT NULL
                      CHECK (horizonte IN ('DIA', 'SEMANA', 'MES', 'LARGO_PLAZO')),
    fecha_referencia  DATE,
    prioridad         VARCHAR(5)    NOT NULL DEFAULT 'MEDIA'
                      CHECK (prioridad IN ('ALTA', 'MEDIA', 'BAJA')),
    estado            VARCHAR(10)   NOT NULL DEFAULT 'PENDIENTE'
                      CHECK (estado IN ('PENDIENTE', 'EN_CURSO', 'FINALIZADA')),
    ambito            VARCHAR(8)    NOT NULL DEFAULT 'PERSONAL'
                      CHECK (ambito IN ('PERSONAL', 'GRUPO')),
    usuario_id        UUID          NOT NULL,
    grupo_id          UUID,
    asignado_a        UUID,
    creada_en         TIMESTAMPTZ   NOT NULL,
    actualizada_en    TIMESTAMPTZ   NOT NULL,
    completada_en     TIMESTAMPTZ,
    CONSTRAINT tarea_fecha_segun_horizonte
        CHECK (horizonte = 'LARGO_PLAZO' OR fecha_referencia IS NOT NULL),
    CONSTRAINT tarea_grupo_segun_ambito
        CHECK ((ambito = 'PERSONAL' AND grupo_id IS NULL) OR (ambito = 'GRUPO' AND grupo_id IS NOT NULL)),
    CONSTRAINT tarea_completada_si_finalizada
        CHECK ((estado = 'FINALIZADA') = (completada_en IS NOT NULL))
);

CREATE INDEX tarea_usuario_horizonte_fecha_idx ON tarea (usuario_id, horizonte, fecha_referencia);
CREATE INDEX tarea_grupo_idx ON tarea (grupo_id) WHERE grupo_id IS NOT NULL;

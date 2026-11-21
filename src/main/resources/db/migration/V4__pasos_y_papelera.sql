-- Checklist: pasos dentro de una tarea (docs/01-especificacion.md §4.7).
CREATE TABLE paso (
    id        UUID          PRIMARY KEY,
    tarea_id  UUID          NOT NULL REFERENCES tarea (id) ON DELETE CASCADE,
    texto     VARCHAR(200)  NOT NULL,
    hecho     BOOLEAN       NOT NULL DEFAULT FALSE,
    orden     INT           NOT NULL
);
CREATE INDEX paso_tarea_idx ON paso (tarea_id, orden);

-- Papelera (§4.8): eliminar marca la tarea; se borra de verdad a los 30 días o al vaciar la papelera.
ALTER TABLE tarea ADD COLUMN eliminada_en TIMESTAMPTZ;
CREATE INDEX tarea_papelera_idx ON tarea (usuario_id, eliminada_en) WHERE eliminada_en IS NOT NULL;

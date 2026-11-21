-- Tareas recurrentes (§4.9): al finalizar una, se crea la siguiente una sola vez.
ALTER TABLE tarea ADD COLUMN repeticion VARCHAR(8) NOT NULL DEFAULT 'NINGUNA'
    CHECK (repeticion IN ('NINGUNA', 'DIARIA', 'HABILES', 'SEMANAL', 'MENSUAL'));
ALTER TABLE tarea ADD COLUMN siguiente_creada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tarea ADD CONSTRAINT tarea_repeticion_con_plazo
    CHECK (repeticion = 'NINGUNA' OR horizonte <> 'SIN_PLAZO');

-- Recordatorios (§4.10): el navegador avisa a esa hora; avisado evita repetir el aviso.
ALTER TABLE tarea ADD COLUMN recordatorio TIMESTAMPTZ;
ALTER TABLE tarea ADD COLUMN recordatorio_avisado BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX tarea_recordatorio_idx ON tarea (usuario_id, recordatorio)
    WHERE recordatorio IS NOT NULL AND recordatorio_avisado = FALSE;

-- Buscador (§4.11): sin distinguir tildes
CREATE EXTENSION IF NOT EXISTS unaccent;

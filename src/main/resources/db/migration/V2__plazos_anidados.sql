-- Plazos anidados (docs/01-especificacion.md §4.1): una tarea puede no tener plazo (SIN_PLAZO, "por planificar"),
-- ser de un día, una semana o un mes, o tener una fecha límite (LIMITE). LARGO_PLAZO deja de existir:
-- sin fecha pasa a SIN_PLAZO y con fecha a LIMITE.
ALTER TABLE tarea DROP CONSTRAINT tarea_horizonte_check;
ALTER TABLE tarea DROP CONSTRAINT tarea_fecha_segun_horizonte;

UPDATE tarea
SET horizonte = CASE WHEN fecha_referencia IS NULL THEN 'SIN_PLAZO' ELSE 'LIMITE' END
WHERE horizonte = 'LARGO_PLAZO';

ALTER TABLE tarea ALTER COLUMN horizonte TYPE VARCHAR(10);
ALTER TABLE tarea ADD CONSTRAINT tarea_horizonte_check
    CHECK (horizonte IN ('SIN_PLAZO', 'DIA', 'SEMANA', 'MES', 'LIMITE'));
ALTER TABLE tarea ADD CONSTRAINT tarea_fecha_segun_horizonte
    CHECK ((horizonte = 'SIN_PLAZO') = (fecha_referencia IS NULL));

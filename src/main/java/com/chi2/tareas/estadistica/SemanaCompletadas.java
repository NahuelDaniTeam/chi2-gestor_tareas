package com.chi2.tareas.estadistica;

import java.time.LocalDate;

/** Cuántas tareas se finalizaron en la semana que empieza en {@code lunes}. */
public record SemanaCompletadas(LocalDate lunes, int cantidad) {
}

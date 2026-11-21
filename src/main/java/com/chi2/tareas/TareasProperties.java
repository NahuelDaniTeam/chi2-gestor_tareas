package com.chi2.tareas;

import java.time.Duration;
import java.time.ZoneId;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code chi2.tareas.*}.
 *
 * @param zona zona horaria con la que se decide qué día es "hoy"
 * @param usuarioLocal dueño de todas las tareas en la etapa 1 (uso personal, sin login)
 * @param retencionPapelera cuánto queda una tarea en la papelera antes de borrarse
 */
@Validated
@ConfigurationProperties("chi2.tareas")
public record TareasProperties(@NotNull ZoneId zona, @NotNull UUID usuarioLocal, @NotNull Duration retencionPapelera) {
}

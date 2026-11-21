package com.chi2.tareas.respaldo;

import java.nio.file.Path;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code chi2.respaldos.*} (§4.15).
 *
 * @param automaticos si se guarda un respaldo JSON por día en {@code carpeta}
 * @param conservar cuántos respaldos diarios se guardan (los más viejos se borran)
 */
@Validated
@ConfigurationProperties("chi2.respaldos")
public record RespaldosProperties(boolean automaticos, @NotNull Path carpeta, @Min(1) int conservar) {
}

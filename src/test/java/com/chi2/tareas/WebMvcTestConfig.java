package com.chi2.tareas;

import java.time.Clock;
import java.time.Instant;

import com.chi2.tareas.tarea.Calendario;
import com.chi2.tareas.tarea.TareasProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Beans que necesitan los tests {@code @WebMvcTest}: calendario real con el reloj fijo en el jueves 19/11/2026. */
@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties(TareasProperties.class)
@Import(Calendario.class)
public class WebMvcTestConfig {

	public static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

	@Bean
	Clock clock() {
		return new RelojDeTest(AHORA);
	}

}

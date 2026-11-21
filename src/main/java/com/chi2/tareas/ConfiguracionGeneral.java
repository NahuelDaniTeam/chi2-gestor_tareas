package com.chi2.tareas;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Beans transversales: reloj inyectable (para testear fechas) y tareas programadas. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ConfiguracionGeneral {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}

package com.chi2.tareas;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans transversales: reloj inyectable (para testear fechas). */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionGeneral {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}

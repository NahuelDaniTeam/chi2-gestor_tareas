package com.chi2.tareas;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GestorTareasApplication {

	public static void main(String[] args) {
		// Todo en UTC: las fechas se guardan en UTC y se convierten a hora local solo al mostrarlas.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(GestorTareasApplication.class, args);
	}

}

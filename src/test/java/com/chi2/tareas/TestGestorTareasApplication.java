package com.chi2.tareas;

import org.springframework.boot.SpringApplication;

/** Levanta la app con un Postgres de Testcontainers, sin compose.yaml. */
public class TestGestorTareasApplication {

	public static void main(String[] args) {
		SpringApplication.from(GestorTareasApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

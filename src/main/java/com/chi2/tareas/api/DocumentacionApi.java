package com.chi2.tareas.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Datos generales de la documentación OpenAPI ({@code /v3/api-docs}, navegable en {@code /api/docs}).
 * Los endpoints y sus esquemas salen de los controllers y los records; el detalle funcional está en
 * docs/01-especificacion.md §8.
 */
@Configuration(proxyBeanMethods = false)
public class DocumentacionApi {

	@Bean
	OpenAPI openApi(@Value("${info.app.version:dev}") String version) {
		return new OpenAPI().info(new Info()
				.title("CHI2 · Gestor de tareas")
				.version(version)
				.description("API REST del gestor de tareas de CHI2. La usa la interfaz web y la va a usar la app nativa. "
						+ "Errores en formato ProblemDetail; los de validación traen un mapa `errores` por campo."));
	}

}

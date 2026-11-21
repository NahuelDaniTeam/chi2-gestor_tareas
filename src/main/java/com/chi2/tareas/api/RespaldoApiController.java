package com.chi2.tareas.api;

import java.nio.charset.StandardCharsets;

import com.chi2.tareas.respaldo.Respaldo;
import com.chi2.tareas.respaldo.RespaldoService;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/exportar} (§4.15): descargar todo en JSON o las tareas en CSV. */
@Tag(name = "Respaldos")
@RestController
@RequestMapping("/api/exportar")
public class RespaldoApiController {

	private final RespaldoService service;

	public RespaldoApiController(RespaldoService service) {
		this.service = service;
	}

	@GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Respaldo> json() {
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, adjunto("json")).body(service.respaldo());
	}

	@GetMapping(value = "/csv", produces = "text/csv")
	public ResponseEntity<byte[]> csv() {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, adjunto("csv"))
				.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
				.body(service.csv().getBytes(StandardCharsets.UTF_8));
	}

	private String adjunto(String extension) {
		return ContentDisposition.attachment().filename(service.nombreArchivo(extension)).build().toString();
	}

}

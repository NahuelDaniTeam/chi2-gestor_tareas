package com.chi2.tareas.api;

import java.util.LinkedHashMap;
import java.util.Map;

import com.chi2.tareas.proyecto.ProyectoDuplicadoException;
import com.chi2.tareas.proyecto.ProyectoNoEncontradoException;
import com.chi2.tareas.tarea.PasoNoEncontradoException;
import com.chi2.tareas.tarea.TareaNoEncontradaException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Traduce las excepciones de la API a HTTP con {@link ProblemDetail}. */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
public class ApiExceptionHandler {

	@ExceptionHandler(BindException.class)
	public ProblemDetail validacion(BindException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Hay datos para corregir.");
		problema.setProperty("errores", erroresPorCampo(ex));
		return problema;
	}

	/** JSON malformado o con un valor fuera de los permitidos (por ejemplo, una prioridad que no existe). */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail cuerpoIlegible() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"No pudimos leer el pedido. Revisá que sea un JSON válido y que los valores existan.");
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class })
	public ProblemDetail parametroInvalido(Exception ex) {
		String parametro = ex instanceof MethodArgumentTypeMismatchException m ? m.getName()
				: ((MissingServletRequestParameterException) ex).getParameterName();
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Falta o no es válido el parámetro '" + parametro + "'.");
	}

	/** Por ejemplo, un rango que termina antes de empezar. */
	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(TareaNoEncontradaException.class)
	public ProblemDetail noEncontrada() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, TareaNoEncontradaException.MENSAJE);
	}

	@ExceptionHandler(PasoNoEncontradoException.class)
	public ProblemDetail pasoNoEncontrado() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, PasoNoEncontradoException.MENSAJE);
	}

	@ExceptionHandler(ProyectoNoEncontradoException.class)
	public ProblemDetail proyectoNoEncontrado() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ProyectoNoEncontradoException.MENSAJE);
	}

	@ExceptionHandler(ProyectoDuplicadoException.class)
	public ProblemDetail proyectoDuplicado(ProyectoDuplicadoException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	private static Map<String, String> erroresPorCampo(BindException ex) {
		Map<String, String> errores = new LinkedHashMap<>();
		for (FieldError error : ex.getFieldErrors()) {
			errores.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		return errores;
	}

}

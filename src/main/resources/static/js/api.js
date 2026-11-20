/** Cliente de /api/tareas. Los errores llegan como ProblemDetail; se convierten en ErrorApi. */

export class ErrorApi extends Error {
	constructor(mensaje, { estado = 0, errores = {} } = {}) {
		super(mensaje);
		this.estado = estado;
		this.errores = errores;
	}

	/** El primer error de validación, o el mensaje general. */
	get primerMensaje() {
		return Object.values(this.errores)[0] || this.message;
	}
}

async function pedir(metodo, url, cuerpo) {
	let respuesta;
	try {
		respuesta = await fetch(url, {
			method: metodo,
			headers: { Accept: "application/json", ...(cuerpo ? { "Content-Type": "application/json" } : {}) },
			body: cuerpo ? JSON.stringify(cuerpo) : undefined
		});
	} catch {
		throw new ErrorApi("No hay conexión con la aplicación.");
	}
	if (respuesta.status === 204) return null;
	const datos = await respuesta.json().catch(() => null);
	if (!respuesta.ok) {
		throw new ErrorApi(datos?.detail || "No se pudo guardar. Probá de nuevo.",
			{ estado: respuesta.status, errores: datos?.errores });
	}
	return datos;
}

export const api = {
	listar: url => pedir("GET", url),
	crear: tarea => pedir("POST", "/api/tareas", tarea),
	editar: (id, tarea) => pedir("PUT", `/api/tareas/${id}`, tarea),
	cambiarPlazo: (id, horizonte, fechaReferencia) => pedir("PATCH", `/api/tareas/${id}/plazo`, { horizonte, fechaReferencia }),
	cambiarEstado: (id, estado) => pedir("PATCH", `/api/tareas/${id}/estado`, { estado }),
	cambiarPrioridad: (id, prioridad) => pedir("PATCH", `/api/tareas/${id}/prioridad`, { prioridad }),
	eliminar: id => pedir("DELETE", `/api/tareas/${id}`),
	proyectos: () => pedir("GET", "/api/proyectos"),
	crearProyecto: proyecto => pedir("POST", "/api/proyectos", proyecto),
	editarProyecto: (id, proyecto) => pedir("PUT", `/api/proyectos/${id}`, proyecto),
	eliminarProyecto: id => pedir("DELETE", `/api/proyectos/${id}`)
};

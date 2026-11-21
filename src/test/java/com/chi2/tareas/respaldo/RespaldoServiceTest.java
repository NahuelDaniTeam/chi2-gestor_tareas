package com.chi2.tareas.respaldo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RespaldoServiceTest {

	@Test
	void lasCeldasSeEscapanComoPideElCsv() {
		assertThat(RespaldoService.celda("Leer capítulo 3")).isEqualTo("Leer capítulo 3");
		assertThat(RespaldoService.celda("uno; dos")).isEqualTo("\"uno; dos\"");
		assertThat(RespaldoService.celda("dijo \"hola\"")).isEqualTo("\"dijo \"\"hola\"\"\"");
		assertThat(RespaldoService.celda("línea 1\nlínea 2")).isEqualTo("\"línea 1\nlínea 2\"");
	}

}

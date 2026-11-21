package com.chi2.tareas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GestorTareasApplicationTests {

	@Test
	void contextLoads() {
	}

}

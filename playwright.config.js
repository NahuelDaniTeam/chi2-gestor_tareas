// Tests de punta a punta (src/test/e2e): la app real, en el navegador, contra un Postgres descartable.
// Levanta TestGestorTareasApplication (Testcontainers) en el puerto 8090, así nunca toca los datos de uso diario.
// Correr: npm run test:e2e  (requiere Docker abierto y el CSS compilado: ./mvnw verify o npm run build:css)
import path from "node:path";

import { defineConfig, devices } from "@playwright/test";

const PUERTO = 8090;
const mvnw = process.platform === "win32" ? `"${path.resolve("mvnw.cmd")}"` : "./mvnw";
const argumentos = [
	`--server.port=${PUERTO}`,
	"--spring.docker.compose.enabled=false",
	"--chi2.respaldos.automaticos=false"
].join(" ");

export default defineConfig({
	testDir: "src/test/e2e",
	fullyParallel: false,
	workers: 1, // comparten la misma base: en serie, para que un test no vea las tareas de otro a mitad de camino
	retries: process.env.CI ? 1 : 0,
	reporter: process.env.CI ? [["github"], ["html", { open: "never" }]] : "list",
	timeout: 30_000,
	use: {
		baseURL: `http://localhost:${PUERTO}`,
		locale: "es-AR",
		timezoneId: "America/Argentina/Cordoba", // la misma zona con la que la app decide qué día es hoy
		trace: "retain-on-failure",
		screenshot: "only-on-failure"
	},
	projects: [
		{ name: "chrome", use: { ...devices["Desktop Chrome"] }, testIgnore: /celular/ },
		{ name: "firefox", use: { ...devices["Desktop Firefox"] }, testIgnore: /celular|accesibilidad/ },
		{ name: "celular", use: { ...devices["Pixel 7"] }, testMatch: /celular|accesibilidad/ }
	],
	webServer: {
		command: `${mvnw} -q spring-boot:test-run -Dskip.npm "-Dspring-boot.run.arguments=${argumentos}"`,
		url: `http://localhost:${PUERTO}/actuator/health`,
		timeout: 240_000,
		reuseExistingServer: !process.env.CI,
		stdout: "ignore",
		stderr: "pipe"
	}
});

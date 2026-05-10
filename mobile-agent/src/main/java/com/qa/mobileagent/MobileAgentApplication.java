package com.qa.mobileagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del módulo {@code mobile-agent} (TASK-I04, RFC-AGENT-01).
 *
 * <p>Empaqueta el {@code CucumberRuntimeEngine} del Core como un servicio HTTP
 * con SSE para que el BE pueda delegar la ejecución vía
 * {@code HttpAgentTransport} (TASK-I03) en una máquina externa con Android SDK
 * / iOS Simulator instalado.
 *
 * <h2>Modo de operación</h2>
 * <ol>
 *   <li>{@code POST /v1/runs}: el cliente envía configuración + contenidos
 *       de archivos {@code .feature}. El agente los materializa en su
 *       workspace local y arranca la ejecución vía
 *       {@link com.qa.common.transport.InProcessTransport}.</li>
 *   <li>{@code GET /v1/runs/&#123;id&#125;/events}: stream SSE con eventos del
 *       reporter (scenario start / step pass / step fail / execution end).</li>
 *   <li>{@code POST /v1/runs/&#123;id&#125;/cancel}: cancela idempotentemente.</li>
 *   <li>{@code GET /v1/capabilities}: devuelve los devices/browsers que el
 *       agente reporta vía SPI.</li>
 * </ol>
 *
 * @author Abel Venero
 * @since TASK-I04
 */
@SpringBootApplication
public class MobileAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MobileAgentApplication.class, args);
    }
}

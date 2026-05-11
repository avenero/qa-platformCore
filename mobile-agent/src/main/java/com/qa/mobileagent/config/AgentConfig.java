package com.qa.mobileagent.config;

import com.qa.common.api.transport.ExecutionTransport;
import com.qa.common.internal.transport.InProcessTransport;
import com.qa.mobileagent.service.FeatureMaterializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Wiring de dependencias del agente: bean {@link ExecutionTransport} basado en
 * {@link InProcessTransport#withDefaults()} (descubre plugins via SPI), y
 * bean {@link FeatureMaterializer} apuntando al workspace configurado.
 *
 * <p>El bean {@code ExecutionTransport} es {@code public} y reemplazable —
 * los tests pueden definir un {@code @Primary} mock para evitar el setup del
 * engine real.
 *
 * @since TASK-I04
 */
@Configuration
public class AgentConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AgentConfig.class);

    private InProcessTransport transport;

    @Bean
    public ExecutionTransport executionTransport() {
        this.transport = InProcessTransport.withDefaults();
        return transport;
    }

    @Bean
    public FeatureMaterializer featureMaterializer(@Value("${agent.workspace}") String workspace) throws IOException {
        Path root = Paths.get(workspace);
        Files.createDirectories(root);
        LOG.info("mobile-agent workspace: {}", root.toAbsolutePath());
        return new FeatureMaterializer(root);
    }

    @PreDestroy
    void shutdown() {
        if (transport != null) {
            try { transport.close(); } catch (RuntimeException ignored) { /* best-effort */ }
        }
    }
}

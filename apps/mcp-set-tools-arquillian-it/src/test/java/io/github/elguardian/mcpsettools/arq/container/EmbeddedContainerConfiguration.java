package io.github.elguardian.mcpsettools.arq.container;

import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

/** No server to configure; tests run in the failsafe JVM that already carries the agent. */
public class EmbeddedContainerConfiguration implements ContainerConfiguration {
    @Override
    public void validate() {}
}

package io.github.elguardian.mcpsettools.arq.container;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * Registers the in-JVM embedded container with Arquillian's service registry.
 * Arquillian discovers {@link DeployableContainer} implementations only through
 * {@link LoadableExtension} registrations, not through raw Java service files.
 */
public class EmbeddedContainerExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, EmbeddedContainer.class);
    }
}

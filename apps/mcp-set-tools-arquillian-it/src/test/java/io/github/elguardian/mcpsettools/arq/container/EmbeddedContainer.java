package io.github.elguardian.mcpsettools.arq.container;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * Minimal in-JVM Arquillian container. The "Local" protocol runs every test method directly in
 * the failsafe fork, which already has the mcp-set-tools agent loaded via {@code argLine}.
 *
 * <p>For WildFly managed containers the equivalent setup is shown in {@code arquillian.xml}:
 * the agent arguments reach WildFly's JVM through {@code javaVmArguments} referencing
 * {@code ${mcpsettools.argLine}}, which is set as a system property by loading the
 * {@code agent.properties} file written by {@code mcp-set-tools:prepare-agent}.
 */
public class EmbeddedContainer implements DeployableContainer<EmbeddedContainerConfiguration> {

    @Override
    public Class<EmbeddedContainerConfiguration> getConfigurationClass() {
        return EmbeddedContainerConfiguration.class;
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Local");
    }

    @Override
    public void setup(EmbeddedContainerConfiguration config) {}

    @Override
    public void start() throws LifecycleException {}

    @Override
    public void stop() throws LifecycleException {}

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        return new ProtocolMetaData();
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {}

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {}

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {}
}

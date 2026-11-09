package io.github.elguardian.mcpsettools.arq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import fixture.Counter;
import io.github.elguardian.mcpsettools.core.MethodId;
import io.github.elguardian.mcpsettools.core.trace.Recording;
import io.github.elguardian.mcpsettools.core.trace.TraceSummary;

/**
 * Verifies that the mcp-set-tools agent instruments and records calls when wired
 * through the standard Arquillian + maven-failsafe-plugin setup.
 *
 * <p>All tests are {@code @RunAsClient}: they execute in the failsafe fork, which has the agent
 * loaded via {@code <argLine>${mcpsettools.argLine}</argLine>}. The same properties file that
 * drives {@code <systemPropertiesFile>} here would drive {@code javaVmArguments} in a
 * WildFly managed container — see {@code arquillian.xml} for the annotated snippet.
 */
@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AgentArqIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "arq-fixture.jar")
                .addClass(Counter.class);
    }

    // ── 1. Agent bootstrap ───────────────────────────────────────────────────

    @Test
    @Order(1)
    @RunAsClient
    void agentIsLoadedViaArgLine() {
        String opts = System.getProperty("mcpsettools.agent.options");
        assertNotNull(opts, "Agent was not loaded — check <argLine>${mcpsettools.argLine}</argLine> in failsafe");
        assertTrue(opts.contains("incl="), "Agent options should contain include patterns: " + opts);
    }

    // ── 2. Properties file is loaded as system properties ───────────────────

    @Test
    @Order(2)
    @RunAsClient
    void propertiesFileIsAvailableAsSysProps() throws Exception {
        // agent.properties is loaded via <systemPropertiesFile>; this is what
        // makes ${mcpsettools.argLine} resolvable in arquillian.xml's javaVmArguments
        // for a managed WildFly container.
        String argLine = System.getProperty("mcpsettools.argLine");
        assertNotNull(argLine,
                "mcpsettools.argLine not found in system properties — "
                        + "check <systemPropertiesFile> in failsafe configuration");
        assertTrue(argLine.contains("-javaagent:"),
                "mcpsettools.argLine should contain -javaagent: but was: " + argLine);

        // Also verify the properties file on disk has correct content
        Path propsFile = Path.of(System.getProperty("mcpsettools.output"), "agent.properties");
        assertTrue(Files.exists(propsFile), "agent.properties not written at " + propsFile);
        Properties p = new Properties();
        try (var in = Files.newInputStream(propsFile)) {
            p.load(in);
        }
        assertTrue(p.getProperty("mcpsettools.argLine", "").contains("-javaagent:"),
                "agent.properties must contain mcpsettools.argLine with -javaagent:");
        assertNotNull(p.getProperty("mcpsettools.agentJar"),
                "agent.properties must contain mcpsettools.agentJar");
    }

    // ── 3. Agent instruments and records fixture calls ───────────────────────

    @Test
    @Order(3)
    @RunAsClient
    void agentRecordsCallsFromInstrumentedClass() throws Exception {
        Path outputDir = Path.of(System.getProperty("mcpsettools.output"));

        // Start a named recording (agent is in record=manual mode)
        Class<?> recorder = Class.forName("io.github.elguardian.mcpsettools.rt.Recorder");
        recorder.getMethod("start", String.class).invoke(null, "arq-test");

        // Drive the instrumented fixture
        Counter counter = new Counter();
        counter.increment();
        counter.increment();
        counter.increment();
        assertEquals(3, counter.getCount());

        // Flush and close the recording
        recorder.getMethod("stop").invoke(null);

        // Verify the trace directory was created
        Path traceDir = outputDir.resolve("arq-test");
        assertTrue(Files.isDirectory(traceDir),
                "Recording directory not found: " + traceDir);

        // Open the recording and check it is complete and contains Counter methods
        Recording rec = Recording.open(traceDir);
        assertTrue(rec.isComplete(), "Recording should be marked complete after stop()");

        TraceSummary summary = TraceSummary.of(rec);
        MethodId increment = summary.stats().keySet().stream()
                .filter(m -> m.owner().endsWith("Counter") && m.name().equals("increment"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No Counter.increment entry in trace; recorded: " + summary.stats().keySet()));

        assertEquals(3, summary.stats().get(increment).calls(),
                "Expected 3 calls to Counter.increment");
    }
}

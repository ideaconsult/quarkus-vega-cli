package net.idea.wrapper.vega.parallel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VegaProcessBuilderTest {

    @Test
    void testCreateProcessForModel(@TempDir Path tempDir) throws IOException {
        String vegaJar = "test.jar";
        String vegaGuiJar = "vega-gui.jar";
        List<String> baseArgs = Arrays.asList(
                "-i", "input.txt",
                "-m", "PLACEHOLDER",
                "-o", "PLACEHOLDER",
                "-f");

        VegaProcessBuilder builder = new VegaProcessBuilder(vegaJar, vegaGuiJar, baseArgs, tempDir, "vega");
        ProcessBuilder pb = builder.createProcessForModel("MELTING_POINT");

        List<String> command = pb.command();

        // Verify command structure uses -cp instead of -jar
        assertEquals("java", command.get(0));
        assertTrue(command.get(1).startsWith("-Djava.util.logging.manager="));
        assertEquals("-cp", command.get(2));
        // Verify classpath contains both JARs
        String classpath = command.get(3);
        assertTrue(classpath.contains("test.jar"));
        assertTrue(classpath.contains("vega-gui.jar"));
        // Verify main class
        assertEquals("io.quarkus.runner.GeneratedMain", command.get(4));
        assertEquals("vega", command.get(5));

        // Verify model is replaced
        assertTrue(command.contains("MELTING_POINT"));
        assertFalse(command.contains("PLACEHOLDER"));

        // Verify output directory path is the base directory (not subdirectory)
        assertTrue(command.contains(tempDir.toString()));
    }

    @Test
    void testCustomCommand(@TempDir Path tempDir) throws IOException {
        String vegaJar = "test.jar";
        String vegaGuiJar = "vega-gui.jar";
        List<String> baseArgs = Arrays.asList("-m", "PLACEHOLDER", "-o", "PLACEHOLDER");
        String customCommand = "custom-command";

        VegaProcessBuilder builder = new VegaProcessBuilder(vegaJar, vegaGuiJar, baseArgs, tempDir, customCommand);
        ProcessBuilder pb = builder.createProcessForModel("MODEL1");
        List<String> command = pb.command();

        assertEquals("java", command.get(0));
        assertTrue(command.get(1).startsWith("-Djava.util.logging.manager="));
        assertEquals("-cp", command.get(2));
        assertEquals("io.quarkus.runner.GeneratedMain", command.get(4));
        assertEquals(customCommand, command.get(5));
    }

    @Test
    void testMultipleModelsUseSameOutputDir(@TempDir Path tempDir) throws IOException {
        String vegaJar = "test.jar";
        String vegaGuiJar = "vega-gui.jar";
        List<String> baseArgs = Arrays.asList("-m", "PLACEHOLDER", "-o", "PLACEHOLDER");

        VegaProcessBuilder builder = new VegaProcessBuilder(vegaJar, vegaGuiJar, baseArgs, tempDir, "vega");

        // Create processes for multiple models
        ProcessBuilder pb1 = builder.createProcessForModel("MODEL1");
        ProcessBuilder pb2 = builder.createProcessForModel("MODEL2");

        // Verify both use the same output directory
        assertTrue(pb1.command().contains(tempDir.toString()));
        assertTrue(pb2.command().contains(tempDir.toString()));
    }

    @Test
    void testValidateVegaJarThrowsExceptionForNonExistent() {
        assertThrows(IOException.class, () -> {
            VegaProcessBuilder.validateVegaJar("nonexistent.jar");
        });
    }

    @Test
    void testValidateVegaJarThrowsExceptionForDirectory(@TempDir Path tempDir) {
        assertThrows(IOException.class, () -> {
            VegaProcessBuilder.validateVegaJar(tempDir.toString());
        });
    }
}

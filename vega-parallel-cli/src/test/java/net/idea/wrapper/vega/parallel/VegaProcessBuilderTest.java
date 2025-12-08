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
        List<String> baseArgs = Arrays.asList(
                "-i", "input.txt",
                "-m", "PLACEHOLDER",
                "-o", "PLACEHOLDER",
                "-f");

        VegaProcessBuilder builder = new VegaProcessBuilder(vegaJar, baseArgs, tempDir, "vega");
        ProcessBuilder pb = builder.createProcessForModel("MELTING_POINT");

        List<String> command = pb.command();

        // Verify command structure
        assertEquals("java", command.get(0));
        assertEquals("-jar", command.get(1));
        assertEquals("test.jar", command.get(2));
        assertEquals("vega", command.get(3));

        // Verify model is replaced
        assertTrue(command.contains("MELTING_POINT"));
        assertFalse(command.contains("PLACEHOLDER"));

        // Verify output directory path is the base directory (not subdirectory)
        assertTrue(command.contains(tempDir.toString()));
    }

    @Test
    void testCustomCommand(@TempDir Path tempDir) throws IOException {
        String vegaJar = "test.jar";
        List<String> baseArgs = Arrays.asList("-m", "PLACEHOLDER", "-o", "PLACEHOLDER");
        String customCommand = "custom-command";

        VegaProcessBuilder builder = new VegaProcessBuilder(vegaJar, baseArgs, tempDir, customCommand);
        ProcessBuilder pb = builder.createProcessForModel("MODEL1");
        List<String> command = pb.command();

        assertEquals("java", command.get(0));
        assertEquals("-jar", command.get(1));
        assertEquals("test.jar", command.get(2));
        assertEquals(customCommand, command.get(3));
    }

    @Test
    void testMultipleModelsUseSameOutputDir(@TempDir Path tempDir) throws IOException {
        String vegaJar = "test.jar";
        List<String> baseArgs = Arrays.asList("-m", "PLACEHOLDER", "-o", "PLACEHOLDER");

        VegaProcessBuilder builder = new VegaProcessBuilder(vegaJar, baseArgs, tempDir, "vega");

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

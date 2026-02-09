package net.idea.wrapper.vega.parallel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds ProcessBuilder instances for executing VEGA CLI as a subprocess.
 */
public class VegaProcessBuilder {

    private final String vegaJarPath;
    private final String vegaGuiJarPath;
    private final List<String> baseArgs;
    private final Path outputDir;
    private final String commandName;

    public VegaProcessBuilder(String vegaJarPath, String vegaGuiJarPath, List<String> baseArgs, Path outputDir,
            String commandName) {
        this.vegaJarPath = vegaJarPath;
        this.vegaGuiJarPath = vegaGuiJarPath;
        this.baseArgs = baseArgs;
        this.outputDir = outputDir;
        this.commandName = commandName;
    }

    /**
     * Create a ProcessBuilder for executing a single model.
     * All models write to the same output directory.
     * 
     * @param modelKey The model key to execute
     * @return ProcessBuilder configured for this model
     * @throws IOException if output directory cannot be created
     */
    public ProcessBuilder createProcessForModel(String modelKey) throws IOException {
        List<String> command = new ArrayList<>();

        // Java command
        command.add("java");

        // Add Quarkus system property
        command.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");

        // Build classpath with both JARs (platform-specific separator)
        String pathSeparator = System.getProperty("path.separator"); // ":" on Unix, ";" on Windows
        String classpath = vegaJarPath + pathSeparator + vegaGuiJarPath;
        command.add("-cp");
        command.add(classpath);

        // Specify main class
        command.add("io.quarkus.runner.GeneratedMain");

        // Add subcommand
        command.add(commandName);

        // Add base arguments, replacing -m value only
        boolean skipNext = false;
        for (int i = 0; i < baseArgs.size(); i++) {
            if (skipNext) {
                skipNext = false;
                continue;
            }

            String arg = baseArgs.get(i);

            // Replace -m or --model with single model key
            if (arg.equals("-m") || arg.equals("--model")) {
                command.add(arg);
                command.add(modelKey);
                skipNext = true;
            }
            // Use the same output directory for all models
            else if (arg.equals("-o") || arg.equals("--output")) {
                // Ensure output directory exists
                Files.createDirectories(outputDir);
                command.add(arg);
                command.add(outputDir.toString());
                skipNext = true;
            }
            // Keep other arguments as-is
            else {
                command.add(arg);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // Merge stderr into stdout

        return pb;
    }

    /**
     * Validate that the VEGA JAR exists.
     */
    public static void validateVegaJar(String vegaJarPath) throws IOException {
        Path jarPath = Paths.get(vegaJarPath);
        if (!Files.exists(jarPath)) {
            throw new IOException("VEGA JAR not found: " + vegaJarPath);
        }
        if (!Files.isRegularFile(jarPath)) {
            throw new IOException("VEGA JAR path is not a file: " + vegaJarPath);
        }
    }

    /**
     * Validate that the Vega-GUI JAR exists.
     */
    public static void validateVegaGuiJar(String vegaGuiJarPath) throws IOException {
        Path jarPath = Paths.get(vegaGuiJarPath);
        if (!Files.exists(jarPath)) {
            throw new IOException("Vega-GUI JAR not found: " + vegaGuiJarPath);
        }
        if (!Files.isRegularFile(jarPath)) {
            throw new IOException("Vega-GUI JAR path is not a file: " + vegaGuiJarPath);
        }
    }
}

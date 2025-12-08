package net.idea.wrapper.vega.parallel;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main CLI command for parallel VEGA execution.
 */
@TopCommand
@Command(name = "vega-parallel", mixinStandardHelpOptions = true, version = "1.0.0", description = "Parallel execution wrapper for VEGA CLI")
public class ParallelVegaCommand implements Runnable {

    // Parallel execution options
    @Option(names = { "--workers", "-w" }, description = "Number of parallel workers (default: number of CPU cores)")
    Integer workers;

    @Option(names = { "--vega-jar" }, description = "Path to VEGA JAR file (default: auto-detect)")
    String vegaJarPath;

    // Original VEGA options (pass-through)
    @Option(names = { "-m",
            "--model" }, required = true, description = "Model key or file with model keys (tab-delimited, first column)")
    String model;

    @Option(names = { "-i", "--input" }, required = true, description = "Path to input SMILES or .txt file")
    String inputFile;

    @Option(names = { "-o", "--output" }, required = true, description = "Output directory for all model results")
    String outputDir;

    @Option(names = { "-s", "--smiles" }, description = "Input SMILES")
    String smiles;

    @Option(names = { "--smilesfield" }, description = "Name of the column with SMILES, default SMILES")
    String smilesField;

    @Option(names = { "--idfield" }, description = "Name of the column with molecule ID, default ID")
    String idField;

    @Option(names = { "-f", "--fastmode" }, description = "Enable fast mode (default: false)")
    boolean fastMode;

    @Option(names = { "-j",
            "--jsonl" }, description = "Write resultw_MODEL.jsonl instead of original resultw_MODEL.txt format")
    boolean jsonl;

    @Option(names = { "-x", "--maxrows" }, description = "Max rows to process from file, default ALL")
    Integer maxRows;

    @Option(names = { "-z",
            "--reinicialize-model" }, description = "Reinitialize model on every N rows. Default -1 (do not reinitialize)")
    Integer reinitializeModel;

    @Option(names = {
            "--timeout" }, description = "Timeout in minutes for single model execution (default: -1, no timeout)")
    Long timeout;

    @picocli.CommandLine.Parameters(index = "0", arity = "0..1", description = "Subcommand to execute (default: vega)", defaultValue = "vega")
    String command;

    @Override
    public void run() {
        try {
            // Determine number of workers
            int numWorkers = (workers != null && workers > 0) ? workers : Runtime.getRuntime().availableProcessors();

            // Determine VEGA JAR path
            String jarPath = determineVegaJarPath();
            VegaProcessBuilder.validateVegaJar(jarPath);

            System.out.println("VEGA Parallel Execution Wrapper");
            System.out.println("================================");
            System.out.println("VEGA JAR: " + jarPath);
            System.out.println("Workers: " + numWorkers);
            System.out.println();

            // Parse models
            List<String> models = parseModels();
            System.out.println("Found " + models.size() + " models to execute");

            // Build base arguments for VEGA CLI
            List<String> baseArgs = buildBaseArguments();

            // Create output directory
            Path outputPath = Paths.get(outputDir);
            Files.createDirectories(outputPath);

            // Execute models in parallel
            VegaProcessBuilder processBuilder = new VegaProcessBuilder(jarPath, baseArgs, outputPath, command);
            long modelTimeout = (timeout != null) ? timeout : -1L;
            ModelExecutor executor = new ModelExecutor(processBuilder, numWorkers, models.size(), modelTimeout);

            boolean success = executor.executeModels(models);

            if (!success) {
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Determine the VEGA JAR path (from option or auto-detect).
     */
    private String determineVegaJarPath() throws IOException {
        if (vegaJarPath != null) {
            return vegaJarPath;
        }

        // Try to auto-detect relative to this JAR
        String[] possiblePaths = {
                "../vega-wrapper-app/target/vega-wrapper-app-1.2.4-SNAPSHOT-runner.jar",
                "./vega-wrapper-app-1.2.4-SNAPSHOT-runner.jar",
                "../vega-wrapper-app-1.2.4-SNAPSHOT-runner.jar"
        };

        for (String path : possiblePaths) {
            Path jarPath = Paths.get(path);
            if (Files.exists(jarPath)) {
                return jarPath.toAbsolutePath().toString();
            }
        }

        throw new IOException("Could not auto-detect VEGA JAR. Please specify with --vega-jar option.");
    }

    /**
     * Parse models from file or single model key.
     */
    private List<String> parseModels() throws IOException {
        Path modelPath = Paths.get(model);

        // Check if it's a file
        if (Files.exists(modelPath) && Files.isRegularFile(modelPath)) {
            return ModelFileParser.parseModelFile(modelPath);
        } else {
            // Single model key
            List<String> models = new ArrayList<>();
            models.add(model);
            return models;
        }
    }

    /**
     * Build base arguments for VEGA CLI (excluding -m and -o which vary per model).
     */
    private List<String> buildBaseArguments() {
        List<String> args = new ArrayList<>();

        // Input
        if (inputFile != null) {
            args.add("-i");
            args.add(inputFile);
        }
        if (smiles != null) {
            args.add("-s");
            args.add(smiles);
        }

        // Fields
        if (smilesField != null) {
            args.add("--smilesfield");
            args.add(smilesField);
        }
        if (idField != null) {
            args.add("--idfield");
            args.add(idField);
        }

        // Modes and options
        if (fastMode) {
            args.add("-f");
        }
        if (jsonl) {
            args.add("-j");
        }

        // Limits
        if (maxRows != null) {
            args.add("-x");
            args.add(maxRows.toString());
        }
        if (reinitializeModel != null) {
            args.add("-z");
            args.add(reinitializeModel.toString());
        }

        // Placeholder for -m and -o (will be replaced per model)
        args.add("-m");
        args.add("PLACEHOLDER");
        args.add("-o");
        args.add("PLACEHOLDER");

        return args;
    }
}

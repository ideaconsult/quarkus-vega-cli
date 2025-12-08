package net.idea.wrapper.vega.parallel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Executes VEGA models in parallel using a worker pool.
 */
public class ModelExecutor {

    private final VegaProcessBuilder processBuilder;
    private final ProgressTracker progressTracker;
    private final int workers;
    private final long timeout;

    public ModelExecutor(VegaProcessBuilder processBuilder, int workers, int totalModels, long timeout) {
        this.processBuilder = processBuilder;
        this.workers = workers;
        this.progressTracker = new ProgressTracker(totalModels);
        this.timeout = timeout;
    }

    /**
     * Execute all models in parallel.
     * 
     * @param models List of model keys to execute
     * @return true if all models completed successfully
     */
    public boolean executeModels(List<String> models) {
        ExecutorService executor = Executors.newFixedThreadPool(workers);

        System.out.println("Starting parallel execution with " + workers + " workers...");
        System.out.println("Total models to process: " + models.size());
        System.out.println();

        // Submit all model execution tasks
        for (String modelKey : models) {
            executor.submit(() -> executeModel(modelKey));
        }

        // Shutdown executor and wait for completion
        executor.shutdown();
        try {
            // Wait for all tasks to complete (indefinitely)
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            System.err.println("Execution interrupted");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }

        progressTracker.printSummary();
        return progressTracker.isSuccess();
    }

    /**
     * Execute a single model.
     */
    private void executeModel(String modelKey) {
        progressTracker.modelStarted(modelKey);

        try {
            ProcessBuilder pb = processBuilder.createProcessForModel(modelKey);
            Process process = pb.start();

            // Capture output (optional - can be disabled for performance)
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Wait for process to complete
            boolean finished;
            if (timeout > 0) {
                finished = process.waitFor(timeout, TimeUnit.MINUTES);
            } else {
                process.waitFor();
                finished = true;
            }

            if (finished) {
                int exitCode = process.exitValue();
                if (exitCode == 0) {
                    progressTracker.modelCompleted(modelKey);
                } else {
                    String errorMsg = "Exit code: " + exitCode;
                    // Include last few lines of output if available
                    String[] lines = output.toString().split("\n");
                    if (lines.length > 0) {
                        errorMsg += " - Last output: " + lines[lines.length - 1];
                    }
                    progressTracker.modelFailed(modelKey, errorMsg);
                }
            } else {
                process.destroyForcibly();
                progressTracker.modelFailed(modelKey, "Timed out after " + timeout + " minutes");
            }

        } catch (IOException e) {
            progressTracker.modelFailed(modelKey, "IO error: " + e.getMessage());
        } catch (InterruptedException e) {
            progressTracker.modelFailed(modelKey, "Interrupted");
            Thread.currentThread().interrupt();
        }
    }
}

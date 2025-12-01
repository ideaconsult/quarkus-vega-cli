package net.idea.wrapper.vega.parallel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks and displays execution progress for parallel model execution.
 */
public class ProgressTracker {

    private final int totalModels;
    private final AtomicInteger completedModels = new AtomicInteger(0);
    private final AtomicInteger failedModels = new AtomicInteger(0);
    private final Map<String, String> runningModels = new ConcurrentHashMap<>();
    private final long startTime;

    public ProgressTracker(int totalModels) {
        this.totalModels = totalModels;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Mark a model as started.
     */
    public void modelStarted(String modelKey) {
        runningModels.put(modelKey, "RUNNING");
        printProgress();
    }

    /**
     * Mark a model as completed successfully.
     */
    public void modelCompleted(String modelKey) {
        runningModels.remove(modelKey);
        completedModels.incrementAndGet();
        printProgress();
    }

    /**
     * Mark a model as failed.
     */
    public void modelFailed(String modelKey, String error) {
        runningModels.remove(modelKey);
        failedModels.incrementAndGet();
        System.err.println("FAILED: " + modelKey + " - " + error);
        printProgress();
    }

    /**
     * Print current progress.
     */
    private void printProgress() {
        int completed = completedModels.get();
        int failed = failedModels.get();
        int running = runningModels.size();

        System.out.printf("\rProgress: %d/%d completed, %d failed, %d running",
                completed, totalModels, failed, running);
        System.out.flush();
    }

    /**
     * Print final summary.
     */
    public void printSummary() {
        System.out.println(); // New line after progress

        long duration = System.currentTimeMillis() - startTime;
        int completed = completedModels.get();
        int failed = failedModels.get();

        System.out.println("\n=== Execution Summary ===");
        System.out.println("Total models: " + totalModels);
        System.out.println("Completed: " + completed);
        System.out.println("Failed: " + failed);
        System.out.println("Duration: " + formatDuration(duration));

        if (failed > 0) {
            System.out.println("\nSome models failed. Check error messages above.");
        }
    }

    /**
     * Format duration in human-readable format.
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Check if all models completed successfully.
     */
    public boolean isSuccess() {
        return failedModels.get() == 0 && completedModels.get() == totalModels;
    }
}

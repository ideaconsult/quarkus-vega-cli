package net.idea.wrapper.vega.parallel;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses model files to extract model keys.
 * Expects tab-delimited format with model keys in the first column.
 */
public class ModelFileParser {

    /**
     * Parse a model file and extract model keys from the first column.
     * 
     * @param modelFilePath Path to the model file
     * @return List of model keys
     * @throws IOException if file cannot be read
     */
    public static List<String> parseModelFile(Path modelFilePath) throws IOException {
        List<String> models = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(modelFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Extract first column (tab-delimited)
                String[] parts = line.split("\t");
                if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                    models.add(parts[0].trim());
                }
            }
        }

        if (models.isEmpty()) {
            throw new IOException("No valid model keys found in file: " + modelFilePath);
        }

        return models;
    }
}

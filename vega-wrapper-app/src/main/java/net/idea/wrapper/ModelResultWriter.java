package net.idea.wrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ModelResultWriter {
    private Map<String, BufferedWriter> writers = new HashMap<>();
    private ObjectMapper mapper = new ObjectMapper();
    private File outputDir;

    public ModelResultWriter(File outputDir) {
        this.outputDir = outputDir;
    }

    public void writeResult(String modelName, Map<String, Object> resultRecord) throws IOException {
        BufferedWriter writer = writers.get(modelName);
        if (writer == null) {
            File file = new File(outputDir, "results_" + modelName + ".jsonl");
            writer = new BufferedWriter(new FileWriter(file, true));
            writers.put(modelName, writer);
        }
        String jsonLine = mapper.writeValueAsString(resultRecord);
        writer.write(jsonLine);
        writer.newLine();
        writer.flush();  // flush after each record to keep it safe
    }

    public void close() throws IOException {
        for (BufferedWriter writer : writers.values()) {
            writer.close();
        }
        writers.clear();
    }
}

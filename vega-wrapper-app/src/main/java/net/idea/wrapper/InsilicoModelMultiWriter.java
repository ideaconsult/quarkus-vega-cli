package net.idea.wrapper;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import insilico.core.model.InsilicoModelOutput;
import insilico.core.model.iInsilicoModel;
import insilico.core.molecule.InsilicoMolecule;


public class InsilicoModelMultiWriter {
    private final File outputDir;
    private final Map<String, InsilicoModelOutputWriter> writers = new HashMap<>();

    public InsilicoModelMultiWriter(File outputDir) {
        this.outputDir = outputDir;
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    /**
     * Get or create a writer for a specific model key.
     */
    public InsilicoModelOutputWriter getWriter(iInsilicoModel model) throws IOException {
        String modelKey = model.getInfo().getKey();
        InsilicoModelOutputWriter writer = writers.get(modelKey);
        if (writer == null) {
            File file = new File(outputDir, "results_" + modelKey + ".txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            writer = new InsilicoModelOutputWriter(bw);
            writers.put(modelKey, writer);
            writer.writeHeader(model);
        }
        return writer;
    }
    /**
     * Write a single InsilicoModelOutput record for the specified modelName and model.
     */
    public void writeResult(int index, iInsilicoModel model, InsilicoModelOutput resultRecord, InsilicoMolecule molecule) throws IOException {
        InsilicoModelOutputWriter writer = getWriter(model);
        writer.writeOne(index, resultRecord, molecule, model);
    }


    /**
     * Close all open writers and clear resources.
     */
    public void close() throws IOException {
        for (InsilicoModelOutputWriter writer : writers.values()) {
            writer.close();
        }
        writers.clear();
    }
}

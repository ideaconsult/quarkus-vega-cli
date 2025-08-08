package net.idea.wrapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import insilico.core.ad.item.iADIndex;
import insilico.core.exception.InitFailureException;
import insilico.core.localization.StringSelectorCore;
import insilico.core.model.InsilicoModel;
import insilico.core.model.InsilicoModelOutput;
import insilico.core.model.iInsilicoModel;
import insilico.core.molecule.InsilicoMolecule;
import insilico.core.tools.utils.ModelUtilities;
import insilico.core.version.InsilicoInfo;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModelResultWriter {
    private Map<String, BufferedWriter> writers = new HashMap<>();
    private ObjectMapper mapper = new ObjectMapper();
    private File outputDir;
    private boolean jsonl = true;
    final static String _tab = "\t";
    public ModelResultWriter(File outputDir) {
        this(outputDir, true);
    }

    public ModelResultWriter(File outputDir, boolean jsonl) {
        this.outputDir = outputDir;
        this.jsonl = jsonl;
    }

    private File getUniqueFile(String baseName, String ext) {
        File file = new File(outputDir, baseName + ext);
        if (!file.exists()) {
            return file;
        }

        int counter = 1;
        File altFile;
        do {
            altFile = new File(outputDir, baseName + "_" + counter + ext);
            counter++;
        } while (altFile.exists());
        return altFile;
    }

    public void writeResult(InsilicoModel model, Map<String, Object> resultRecord, int index) throws IOException {
        String modelName = model.getInfo().getKey();
        BufferedWriter writer = writers.get(modelName);
        if (writer == null) {
            String ext = this.jsonl ? ".jsonl" : ".txt";
            String baseName = "resultsw_" + modelName;
            File file = getUniqueFile(baseName, ext);

            writer = new BufferedWriter(new FileWriter(file, false));
            writers.put(modelName, writer);
        }        
        if (this.jsonl) {
            String jsonLine = mapper.writeValueAsString(resultRecord);
            writer.write(jsonLine);
            writer.newLine();
        } else {
            if (index==1) {
                writeHeader( model, resultRecord, writer);
            }
            boolean first = true;
            for (Object value : resultRecord.values()) {
                if (!first) writer.write(_tab);
                writer.write(value != null ? value.toString() : "-");
                first = false;
            }
            writer.newLine();
        }
        writer.flush();  // flush after each record to keep it safe
    }

    public void writeHeader(iInsilicoModel model, Map<String, Object> resultRecord, BufferedWriter writer) throws IOException {
        
        writer.write(StringSelectorCore.getString("report_txt_intro") + System.lineSeparator());
        String var10001 = String.format(StringSelectorCore.getString("report_txt_version"), model.getInfo().getName(), model.getInfo().getVersion());
        writer.write(var10001 + System.lineSeparator());

        try {
            InsilicoInfo icv = new InsilicoInfo();
            var10001 = String.format(StringSelectorCore.getString("report_txt_core_version"), icv.getVersion());
            writer.write(var10001 + System.lineSeparator());
        } catch (InitFailureException var10) {
            writer.write(System.lineSeparator());
            System.err.println(String.format(StringSelectorCore.getString("report_txt_init_exception"), var10.getMessage()));
        }
        writer.write(System.lineSeparator());
        boolean first = true;
        for (Object key : resultRecord.keySet()) {
            if (!first) writer.write(_tab);
            writer.write(key.toString());
            first = false;
        }
        writer.write(System.lineSeparator());
        writer.flush();
    }

    public void close() throws IOException {
        for (BufferedWriter writer : writers.values()) {
            writer.close();
        }
        writers.clear();
    }

    public static Map<String, Object> toRecord(int index, InsilicoModelOutput output,
                         InsilicoMolecule molecule, iInsilicoModel model) throws IOException {
        Map<String, Object> record = new LinkedHashMap<>(); // preserves order

        // Core columns
        record.put("No.", index);
        record.put("ID", output.getMoleculeId());
        record.put("SMILES", output.getMoleculeSMILES());

        if (output.getStatus() < 1) {
            record.put("Assessment", "[" + StringSelectorCore.getString("report_txt_struct_error") + "]");
            for (String name : model.GetResultsName()) record.put(name, "-");
            record.put(StringSelectorCore.getString("report_txt_experimental"), "-");
            if (model.getInfo().hasAlerts()) {
                record.put(StringSelectorCore.getString("report_txt_struct_alerts"), "-");
            }
            record.put(StringSelectorCore.getString("report_txt_adi"), "-");
            for (String adiName : model.GetADItemsName()) record.put(adiName, "-");
        } else {
            record.put("Assessment", output.getAssessment());
            String[] results = output.getResults();
            String[] resultNames = model.GetResultsName();
            for (int i = 0; i < resultNames.length; i++) {
                record.put(resultNames[i], results[i]);
            }
            record.put(StringSelectorCore.getString("report_txt_experimental"), output.getExperimentalFormatted());

            if (model.getInfo().hasAlerts()) {
                record.put(StringSelectorCore.getString("report_txt_struct_alerts"),
                           ModelUtilities.BuildSANameList(output.getSAList().getSAList()));
            }

            if (output.getStatus() != 2) {
                record.put(StringSelectorCore.getString("report_txt_adi"), output.getADI().GetIndexValueFormatted());
                Iterator<iADIndex> adIndexIter = output.getADIndex().iterator();
                for (String adiName : model.GetADItemsName()) {
                    record.put(adiName, adIndexIter.hasNext()
                        ? adIndexIter.next().GetIndexValueFormatted()
                        : "-");
                }
            } else {
                record.put(StringSelectorCore.getString("report_txt_adi"), "-");
                for (String adiName : model.GetADItemsName()) record.put(adiName, "-");
            }
        }

        // Remarks / messages
        StringBuilder msg = new StringBuilder();
        for (int w = 0; w < molecule.GetWarnings().GetSize(); w++) {
            msg.append("[").append(StringSelectorCore.getString("report_txt_molecule_warning")).append("]")
               .append(molecule.GetWarnings().GetMessages(w)).append(". ");
        }
        for (int e = 0; e < molecule.GetErrors().GetSize(); e++) {
            msg.append("[").append(StringSelectorCore.getString("report_txt_molecule_error")).append("]")
               .append(molecule.GetErrors().GetMessages(e)).append(". ");
        }
        if (!output.getErrMessage().isEmpty()) {
            msg.append("[").append(StringSelectorCore.getString("report_txt_model")).append("]")
               .append(output.getErrMessage()).append(".");
        }
        if (msg.length() == 0) msg.append("-");
        record.put(StringSelectorCore.getString("report_txt_struct_remarks"), msg.toString());
        record.put("STATUS", output.getStatus());
        return record;
    }    
}

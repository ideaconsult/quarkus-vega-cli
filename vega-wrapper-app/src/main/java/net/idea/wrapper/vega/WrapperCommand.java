package net.idea.wrapper.vega;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;

import insilico.core.model.InsilicoModel;
import insilico.core.model.InsilicoModelOutput;
import insilico.core.model.iInsilicoModel;
import insilico.core.model.report.txt.ReportTXTSingle;
import insilico.core.model.runner.InsilicoModelRunnerByMolecule;
import insilico.core.model.runner.InsilicoModelWrapper;
import insilico.core.molecule.InsilicoMolecule;
import insilico.core.molecule.conversion.SmilesMolecule;
import net.idea.wrapper.ModelResultWriter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


@Command(name = "vega", mixinStandardHelpOptions = true,
         description = "Wraps the VEGA-GUI.jar functionality")
public class WrapperCommand implements Callable<Integer> {

    static class InputGroup {
        @Option(names = {"-s", "--smiles"}, description = "Input SMILES")
        String smiles;

        @Option(names = {"-i", "--input"}, description = "Path to input SMILES or .txt file")
        File inputFile;
    }
    

    @ArgGroup(exclusive = true, multiplicity = "1")
    InputGroup inputGroup;

    @Option(names = {"--smilesfield"}, description = "Name of the column with SMILES")
    String smilesField;

    @Option(names = {"--idfield"}, description = "Name of the column with molecule ID")
    String idField;    

    @Option(names = {"-m", "--model"}, description = "Model key", required = true)
    String modelKey;

    @Option(names = {"-o", "--output"}, description = "Output report folder", required = true)
    File outputDir;

    @Option(names = {"-f", "--fastmode"},
            description = "Enable fast mode (default: false)")
    boolean fastmode = false;

    @Option(names = {"-l", "--list-models"}, description = "List available models", help = true)
    boolean listModels;
        
    public void listAll() throws Exception {
        System.out.println("list models");
        ServiceLoader<iInsilicoModel> loader = ServiceLoader.load(iInsilicoModel.class);

        for (iInsilicoModel impl : loader) {
            System.out.println("Discovered: " + impl.getClass().getName());
            // impl.doSomething();
        }
    }
    @Override
    public Integer call() throws Exception {
        try {
            if (listModels) {
                ImplScanner.list_models(false, outputDir);
                return 0;
            } else {
                if (!outputDir.exists()) {
                    if (!outputDir.mkdirs()) {
                        throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
                    }
                } else if (!outputDir.isDirectory()) {
                    throw new IllegalArgumentException("Provided output path is not a directory: " + outputDir.getAbsolutePath());
                }

                InsilicoModel model = ModelRegistry.getModelByKey(modelKey);
                // InsilicoModel model = (InsilicoModel) Class.forName(classname).getDeclaredConstructor().newInstance();
                if (fastmode) {
                    return run_fast(
                                model,
                                inputGroup.inputFile,
                                outputDir,
                                smilesField,
                                idField
                        );          
                } else {
                    ArrayList<InsilicoMolecule> dataset = new ArrayList<InsilicoMolecule>();
                    InsilicoMolecule mol = SmilesMolecule.Convert(inputGroup.smiles.trim());
                    dataset.add(mol);
                    return run_vega(model , dataset, outputDir);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    private int run_vega( InsilicoModel model, ArrayList<InsilicoMolecule> dataset, File outputDir) throws Exception {
        try {
            try { 
                InsilicoModelRunnerByMolecule runner = new InsilicoModelRunnerByMolecule();
                runner.AddModel(model);
                runner.Run(dataset);
                for (InsilicoModelWrapper curModel : runner.GetModelWrappers()) {
                    File outputFile = new File(outputDir, "report_" + model.getInfo().getKey()  + ".txt");
                    ReportTXTSingle.PrintReport(dataset, curModel, new PrintWriter(outputFile));
                }

            } catch (Exception x) {
                System.err.println(x);
                
            }                
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    private int run_fast(
            InsilicoModel model,
            File inputFile,
            File outputDir,
            String smilesFieldName,
            String idFieldName
    ) throws Exception {
        ModelResultWriter resultWriter = new ModelResultWriter(outputDir);

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("Input file is empty.");
            }

            String[] headers = headerLine.split("\t");
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                System.out.println(headers[i].trim());
                headerIndex.put(headers[i].trim(), i);
            }
            
            Integer smilesIndex = headerIndex.get(smilesFieldName);
            Integer idIndex = headerIndex.get(idFieldName);

            if (smilesIndex == null || idIndex == null) {
                throw new IllegalArgumentException("Missing required fields: " +
                        smilesFieldName + " or " + idFieldName);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] fields = line.split("\t", -1); // include trailing empty fields
                if (fields.length <= Math.max(smilesIndex, idIndex)) continue;

                String smiles = fields[smilesIndex];
                String id = fields[idIndex];


                InsilicoMolecule mol = SmilesMolecule.Convert(smiles); 
                mol.SetId(id);
                InsilicoModelOutput output = model.Execute(mol);

                Map<String, Object> record = new HashMap<>();
                record.put("smiles", output.getMoleculeSMILES());
                record.put("id", output.getMoleculeId());
                record.put("model", model.getInfo().getKey());
                record.put("assessment", output.getAssessment());

                String[] resultNames = model.GetResultsName();
                Object[] resultValues = output.getResults();
                for (int i = 0; i < resultNames.length; i++) {
                    record.put(resultNames[i], resultValues[i]);
                }

                resultWriter.writeResult(model.getInfo().getKey(), record);
            }
        }

        resultWriter.close();
        return 0;
    }

}

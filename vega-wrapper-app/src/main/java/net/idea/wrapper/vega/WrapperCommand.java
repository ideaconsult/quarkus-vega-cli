package net.idea.wrapper.vega;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import insilico.core.model.InsilicoModel;
import insilico.core.model.InsilicoModelOutput;
import insilico.core.model.iInsilicoModel;
import insilico.core.model.report.txt.ReportTXTSingle;
import insilico.core.model.runner.InsilicoModelRunnerByMolecule;
import insilico.core.model.runner.InsilicoModelWrapper;
import insilico.core.model.runner.iInsilicoModelRunnerMessenger;
import insilico.core.molecule.InsilicoMolecule;
import insilico.core.molecule.conversion.SmilesMolecule;
import insilico.core.molecule.conversion.file.MoleculeFileSmiles;
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

    @Option(names = {"--smilesfield"}, description = "Name of the column with SMILES, default SMILES")
    String smilesField = "SMILES";

    @Option(names = {"--idfield"}, description = "Name of the column with molecule ID, default ID")
    String idField = "ID";    

    @Option(names = {"-m", "--model"}, description = "Model key (see --list-models) or file with model keys", required = true)
    String modelKey;

    @Option(names = {"-o", "--output"}, description = "Output report folder", required = true)
    File outputDir;

    @Option(names = {"-f", "--fastmode"},
            description = "Enable fast mode (default: false)")
    boolean fastmode = false;

    @Option(names = {"-x", "--maxrows"}, description = "Max rows to process from file, default ALL", required = false)
    int maxRows = -1;

    @Option(names = {"-l", "--list-models"}, description = "List available models. Specify output file e.g. -o models.txt", help = true)
    boolean listModels;

        
    public void listAll() throws Exception {
        System.out.println("list models");
        ServiceLoader<iInsilicoModel> loader = ServiceLoader.load(iInsilicoModel.class);

        for (iInsilicoModel impl : loader) {
            System.out.println("Discovered: " + impl.getClass().getName());
            // impl.doSomething();
        }
    }

    protected int runModel(String modelKey) throws Exception {
        try {
            long startTime = System.nanoTime();
            InsilicoModel model = ModelRegistry.getModelByKey(modelKey);
            long endTime = System.nanoTime();
            long elapsedNano = endTime - startTime;
            double elapsedSeconds = elapsedNano / 1_000_000_000.0;                
            System.out.printf("Loading model %s in: %.2f seconds%n", modelKey, elapsedSeconds);

            int rowNum = 0;
            if (inputGroup.inputFile == null) fastmode = false;
            startTime = System.nanoTime();
            if (fastmode) {
                System.out.println("Processing in fast mode ...");
                rowNum = run_fast(
                            model,
                            inputGroup.inputFile,
                            outputDir,
                            smilesField,
                            idField
                    );
                System.out.println("\nDone.");
            } else {
                ArrayList<InsilicoMolecule> dataset = null;
                if (inputGroup.smiles != null) {
                    dataset = new ArrayList<InsilicoMolecule>();
                    InsilicoMolecule mol = SmilesMolecule.Convert(inputGroup.smiles.trim());
                    dataset.add(mol);
                } else {
                    int[] positions = WrapperCommand.getFieldPositions(
                        inputGroup.inputFile, smilesField, idField);
                    int smilesPos = positions[0];
                    int idPos = positions[1]; // can be -1   
                    System.out.printf("%s %d %s %d", smilesField, smilesPos,idField, idPos);
                    MoleculeFileSmiles SMIReader = new MoleculeFileSmiles();
                    SMIReader.setCASField(-1);
                    SMIReader.setIdField(idPos);
                    SMIReader.setSmilesField(smilesPos);
                    SMIReader.OpenFile(inputGroup.inputFile.getAbsolutePath());
                    dataset = SMIReader.ReadAll();
                    SMIReader.CloseFile();                        
                    System.out.println("Loaded  "+ inputGroup.inputFile);
                }
                
                rowNum = run_vega(model , dataset, outputDir);
            }
            endTime = System.nanoTime();
            elapsedNano = endTime - startTime;
            elapsedSeconds = elapsedNano / 1_000_000_000.0;                 
            if (rowNum > 0) {
                double avgPerRow = elapsedSeconds / rowNum;
                System.out.printf("Elapsed time: %.2f seconds%n", elapsedSeconds);
                System.out.printf("Average time per row: %.4f seconds%n", avgPerRow);
            } else {
                System.out.println("No rows processed.");
            }                
            return rowNum>0?0:1;
        }  catch (Exception x) {
            throw x;
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
                File file_models = new File(modelKey);
                if (file_models.exists()) {
                    List<String> model_keys = Files.lines(file_models.toPath(), StandardCharsets.UTF_8)
                                .skip(1)
                                .map(line -> line.split("\t")[0])
                                .collect(Collectors.toList());                   
                    int rownum = 0;
                    for (String model_key:model_keys) {
                        rownum += runModel(model_key);
                    }
                    return rownum;
                } else {    
                    return runModel(modelKey);
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
                // Messenger for progress bar
                iInsilicoModelRunnerMessenger Messenger = new iInsilicoModelRunnerMessenger() {
                    int rowNum = 0;
                    @Override
                    public void SendMessage(String msg) {
                        System.out.printf("%s\n", msg);
                        System.out.flush();
                    }

                    @Override
                    public void UpdateProgress() {
                        rowNum++;
                        System.out.print("\rProcessed rows: " + rowNum);
                        System.out.flush();
                    } 
                };
                runner.setMessenger(Messenger);                
                runner.AddModel(model);
                runner.getMessenger().SendMessage("Processing...");
                runner.Run(dataset);
                for (InsilicoModelWrapper curModel : runner.GetModelWrappers()) {
                    File outputFile = new File(outputDir, "report_" + model.getInfo().getKey()  + ".txt");
                    System.out.println("Writing to  "+ outputFile);                    
                    ReportTXTSingle.PrintReport(dataset, curModel, new PrintWriter(outputFile));
                }

            } catch (Exception x) {
                System.err.println(x);
                
            }                
            return dataset.size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int run_fast(
            InsilicoModel model,
            File inputFile,
            File outputDir,
            String smilesFieldName,
            String idFieldName
    ) throws Exception {
        int rowNum = 0; 
        ModelResultWriter resultWriter = new ModelResultWriter(outputDir);
        // InsilicoModelRunnerByMolecule runner = new InsilicoModelRunnerByMolecule();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            System.out.println("Reading "+ inputFile);
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("Input file is empty.");
            }

            String[] headers = headerLine.split("\t");
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                System.out.print(headers[i].trim());
                System.out.print("\t");
                headerIndex.put(headers[i].trim(), i);
            }
            System.out.println();
            
            Integer smilesIndex = headerIndex.get(smilesFieldName);
            Integer idIndex = headerIndex.get(idFieldName);

            if (smilesIndex == null || idIndex == null) {
                throw new IllegalArgumentException("Missing required fields: " +
                        smilesFieldName + " or " + idFieldName);
            }
            System.out.println("Writing to  "+ outputDir);
   
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                rowNum++;
                //if (rowNum % 2 == 0) {
                System.out.print("\rProcessed rows: " + rowNum);
                System.out.flush();
                //}                
                String[] fields = line.split("\t", -1); // include trailing empty fields
                if (fields.length <= Math.max(smilesIndex, idIndex)) continue;

                String smiles = fields[smilesIndex];
                String id = fields[idIndex];


                InsilicoMolecule mol = SmilesMolecule.Convert(smiles); 
                mol.SetId(id);
                // runner.Run(mol);
                InsilicoModelOutput output = model.Execute(mol);

                Map<String, Object> record = new HashMap<>();
                record.put("smiles", output.getMoleculeSMILES());
                record.put("id", output.getMoleculeId());
                record.put("model", model.getInfo().getKey());
                record.put("assessment", output.getAssessment());

                String[] resultNames = model.GetResultsName();
                Object[] resultValues = output.getResults();
                if (resultValues != null)
                    for (int i = 0; i < resultNames.length; i++) {
                        record.put(resultNames[i], resultValues[i]);
                    }

                resultWriter.writeResult(model.getInfo().getKey(), record);
                if ((maxRows>0) & (rowNum>=maxRows)) break;
            }
        }

        resultWriter.close();
        return rowNum;
    }

    public static int[] getFieldPositions(File inputFile, String smilesField, String idField) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String[] headers = reader.readLine().split("\t");
            int smilesIdx = -1, idIdx = -1;

            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase(smilesField)) smilesIdx = i;
                if (headers[i].trim().equalsIgnoreCase(idField)) idIdx = i;
            }

            if (smilesIdx == -1) throw new IllegalArgumentException("SMILES field not found: " + smilesField);
            return new int[]{smilesIdx, idIdx}; // idIdx may be -1 if not found or not needed
        }
    }

}

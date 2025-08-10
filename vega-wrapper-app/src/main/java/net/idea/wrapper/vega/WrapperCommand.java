package net.idea.wrapper.vega;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.ibm.icu.text.CharsetDetector;

import insilico.core.ad.item.iADIndex;
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
import net.idea.wrapper.StreamingMeanLong;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.logging.Level;
import java.util.logging.Logger;

@Command(name = "vega", mixinStandardHelpOptions = true,
         description = "Wraps the VEGA-GUI.jar functionality")
public class WrapperCommand implements Callable<Integer> {
    // Create a logger for this class
    private static final Logger logger = Logger.getLogger(WrapperCommand.class.getName());

    StreamingMeanLong freeMemory = new StreamingMeanLong();
    StreamingMeanLong usedMemory = new StreamingMeanLong();
    StreamingMeanLong modelTimePerRecord = new StreamingMeanLong();
    StreamingMeanLong globalTimePerRecord = new StreamingMeanLong();
    

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

    @Option(names = {"-o", "--output"}, description = "Output report folder, or output file if -l", required = true)
    File outputDir;

    @Option(names = {"-f", "--fastmode"},
            description = "Enable fast mode (default: false)")
    boolean fastmode = false;

    @Option(names = {"-j", "--jsonl"},
    description = "Write resultw_MODEL.jsonl instead of original resultw_MODEL.txt format in fastmode")
    boolean jsonl = false;

    @Option(names = {"-z", "--reinicialize-model"},
            description = "Reinitialize model on every N rows. Default -1 (do not reinitialize)")
     private int reinicializeModel=-1;

    @Option(names = {"-x", "--maxrows"}, description = "Max rows to process from file, default ALL", required = false)
    int maxRows = -1;

    @Option(names = {"-l", "--list-models"}, description = "List available models. Specify output file e.g. -o models.txt", help = true)
    boolean listModels;

        
    public void listAll() throws Exception {
        logger.info("list models");
        ServiceLoader<iInsilicoModel> loader = ServiceLoader.load(iInsilicoModel.class);

        for (iInsilicoModel impl : loader) {
            logger.info("Discovered: " + impl.getClass().getName());
            // impl.doSomething();
        }
    }

    public String printMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long _freeMemory = runtime.freeMemory();
        long _usedMemory = totalMemory - _freeMemory;
        
        String msg = String.format("Memory (used/free): %d/%d MB Time per molecule: %.4f s",
            _usedMemory / (1024 * 1024),
            _freeMemory / (1024 * 1024),            
            modelTimePerRecord.getMean() / 1_000_000_000.0
        );

        this.usedMemory.add(_usedMemory / (1024 * 1024));
        this.freeMemory.add(_freeMemory / (1024 * 1024));
        return msg;

    }
    protected int runModel(String modelKey) throws Exception {
        try {
            long startTime = System.nanoTime();
            InsilicoModel model = ModelRegistry.getModelByKey(modelKey);
            long endTime = System.nanoTime();
            long elapsedNano = endTime - startTime;
            double elapsedSeconds = elapsedNano / 1_000_000_000.0;                
            logger.fine(String.format("Loading model %s in: %.2f seconds%n", modelKey, elapsedSeconds));

            int rowNum = 0;
            if (inputGroup.inputFile == null) fastmode = false;
            startTime = System.nanoTime();
            if (fastmode) {
                System.out.println();
                logger.info(String.format("[%s] Processing in fast mode ...", modelKey));
                rowNum = run_fast(
                            model,
                            inputGroup.inputFile,
                            outputDir,
                            smilesField,
                            idField,
                            !jsonl
                    );
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
                    logger.fine(String.format("%s %d %s %d", smilesField, smilesPos,idField, idPos));
                    MoleculeFileSmiles SMIReader = new MoleculeFileSmiles();
                    SMIReader.setCASField(-1);
                    SMIReader.setIdField(idPos);
                    SMIReader.setSmilesField(smilesPos);
                    SMIReader.OpenFile(inputGroup.inputFile.getAbsolutePath());
                    dataset = SMIReader.ReadAll();
                    SMIReader.CloseFile();                        
                    logger.log(Level.FINE,"Loaded  "+ inputGroup.inputFile);
                }
                
                rowNum = run_vega(model , dataset, outputDir);
                
            }

            endTime = System.nanoTime();
            elapsedNano = endTime - startTime;
            elapsedSeconds = elapsedNano / 1_000_000_000.0;
            System.out.print("\r--------------------------------------------------------------------------------------------------\n");
            System.out.flush();
            if (rowNum > 0) {
                logger.info(String.format("[%s] Processed: %d rows Elapsed time: %.2f s Average time per molecule: %.4f s", 
                    model.getInfo().getKey(), rowNum, elapsedSeconds, globalTimePerRecord.getMean() /  1_000_000_000.0 ));
            } else {
                logger.log(Level.WARNING,"No rows processed.");
            }         
            if (this.freeMemory.getCount()==0)
                printMemoryUsage();
            logger.info(String.format(
            "[%s] Average Memory (used/free): %d/%d MB",
            model.getInfo().getKey(),
            this.usedMemory.getMean(),
            this.freeMemory.getMean()            
            ));
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
                    List<String> model_keys = readKeys(file_models.toPath());
                    int rownum = 0;
                    for (String model_key:model_keys) try {
                        logger.fine(String.format("Running %s",model_key));
                        this.modelTimePerRecord = new StreamingMeanLong();
                        this.freeMemory = new StreamingMeanLong();
                        this.usedMemory = new StreamingMeanLong();
                        rownum += runModel(model_key.trim());
                    } catch (Exception x) {
                        logger.log(Level.SEVERE, model_key, x);
                    }
                    return rownum;
                } else try {    
                    this.modelTimePerRecord = new StreamingMeanLong();
                    this.freeMemory = new StreamingMeanLong();
                    this.usedMemory = new StreamingMeanLong();                    
                    return runModel(modelKey);
                }  catch (Exception x) {
                    logger.log(Level.SEVERE, modelKey, x);
                    throw x;
                }

            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error" , e);
            return 1;
        }
    }

    private int run_vega( InsilicoModel model, ArrayList<InsilicoMolecule> dataset, File outputDir) throws Exception {
        // long startTime = System.nanoTime();
        try {
            try { 
                InsilicoModelRunnerByMolecule runner = new InsilicoModelRunnerByMolecule();
                // Messenger for progress bar
                iInsilicoModelRunnerMessenger Messenger = new iInsilicoModelRunnerMessenger() {
                    int rowNum = 0;
                    @Override
                    public void SendMessage(String msg) {
                        System.out.printf("%s", msg);
                        System.out.flush();
                    }

                    @Override
                    public void UpdateProgress() {
                        rowNum++;
                        System.out.print("\rProcessed rows: " + rowNum);
                        if (rowNum % 100 == 0) {
                            System.out.print(" " + printMemoryUsage());
                        }
                        System.out.flush();
                        
                    } 
                };
                runner.setMessenger(Messenger);                
                runner.AddModel(model);
                runner.getMessenger().SendMessage("Processing...");
                runner.Run(dataset);
                for (InsilicoModelWrapper curModel : runner.GetModelWrappers()) {
                    File outputFile = new File(outputDir, "report_" + model.getInfo().getKey()  + ".txt");
                    logger.fine("Writing to  "+ outputFile);                    
                    ReportTXTSingle.PrintReport(dataset, curModel, new PrintWriter(outputFile));
                }

            } catch (Exception x) {
                logger.log(Level.SEVERE, model.getInfo().getKey(), x);
                
            }                
            return dataset.size();
        } catch (Exception e) {
            logger.log(Level.SEVERE, model.getInfo().getKey(), e);
            return 0;
        } finally {
            // long endTime = System.nanoTime();
            //long elapsedNano = endTime - startTime;
            //modelTimePerRecord.add(elapsedNano);
            //globalTimePerRecord.add(elapsedNano);
        }
    }

    private int run_fast(
            InsilicoModel model,
            File inputFile,
            File outputDir,
            String smilesFieldName,
            String idFieldName,
            Boolean originalFormat
    ) throws Exception {
        int rowNum = 0; 
        ModelResultWriter resultWriter = null;
        resultWriter = new ModelResultWriter(outputDir, ! originalFormat);
        String lastMemoryInfo = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            logger.fine("Reading "+ inputFile);
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException(String.format("Input file %s is empty.", inputFile.getAbsolutePath()));
            }

            String[] headers = headerLine.split("\t");
            Map<String, Integer> headerIndex = new HashMap<>();
            StringBuilder _log= new StringBuilder();
            for (int i = 0; i < headers.length; i++) {
                _log.append(headers[i].trim());
                _log.append("\t" );
                headerIndex.put(headers[i].trim(), i);
            }
            logger.finer(_log.toString());
            
            Integer smilesIndex = headerIndex.get(smilesFieldName);
            Integer idIndex = headerIndex.get(idFieldName);
            
            if (idIndex == null) idIndex = -1;

            if (smilesIndex == null) {
                throw new IllegalArgumentException("Missing required field: " +
                        smilesFieldName );
            }
            logger.fine(String.format("Writing resultsw_%s%s to %s",
            model.getInfo().getKey(),jsonl?".jsonl":".txt",outputDir));
   
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                rowNum++;                
                if (reinicializeModel > 2) {    
                    if (rowNum % reinicializeModel == 0) {
                        logger.info("Reinitialize Model at: " + rowNum);
                        Class cls = model.getClass();
                        model = null;
                        model = (InsilicoModel) cls.getDeclaredConstructor().newInstance();
                        logger.info(String.format("[%s] %s", model.getInfo().getKey(), printMemoryUsage()));
                    }
                }                  

                if (rowNum % 100 == 0) {
                    lastMemoryInfo = printMemoryUsage();
                }                
                System.out.print("\rProcessed rows: " + rowNum + "  " + lastMemoryInfo + "    ");
                System.out.flush();
            

                long startTime = System.nanoTime();
                String[] fields = line.split("\t", -1); // include trailing empty fields
                if (fields.length <= Math.max(smilesIndex, idIndex)) continue;

                String smiles = fields[smilesIndex];
                InsilicoMolecule mol = null;
                Map<String, Object> record = null;                
                try {
                    mol = SmilesMolecule.Convert(smiles); 
                    if (idIndex != null & idIndex>=0)
                        mol.SetId(String.format("%s",fields[idIndex]));   
                    else
                        mol.SetId(String.format("%d",rowNum));                 
                    InsilicoModelOutput output = model.Execute(mol);
                    record = ModelResultWriter.toRecord(rowNum, output,mol, model);
                    resultWriter.writeResult(model, record, rowNum);

                } catch (Exception x) {
                    logger.log(Level.SEVERE, "Unexpected error occurred", x);
                    /*
                    record = new HashMap<>();  
                    record.put("ERROR", x.getMessage()); 
                    record.put("SMILES", smiles);
                    record.put("ID", idIndex);
                    record.put("MODEL", model.getInfo().getKey());
                    record.put("Status", -1);                     

                    if (originalFormat)
                        txtWriter.writeResult(rowNum, model, null , mol);
                    else {
                        record = new HashMap<>();  
                        record.put("ERROR", x.getMessage()); 
                        record.put("SMILES", smiles);
                        record.put("ID", idIndex);
                        record.put("MODEL", model.getInfo().getKey());
                        record.put("Status", -1);
                        resultWriter.writeResult(modelKey, record);
                    }
                                             */
                } finally {
                    long endTime = System.nanoTime();
                    long elapsedNano = endTime - startTime;
                    modelTimePerRecord.add(elapsedNano);
                    globalTimePerRecord.add(elapsedNano);
                    
                }                
                // resultWriter.writeResult(model.getInfo().getKey(), record);
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



    public static List<String> readKeys(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            logger.warning("File not found: " + filePath);
            return Collections.emptyList();
        }

        // Read all bytes from the file
        byte[] fileBytes = Files.readAllBytes(filePath);

        // Use CharsetDetector to detect the most likely encoding
        CharsetDetector detector = new CharsetDetector();
        detector.setText(fileBytes);
        
        // Get the best match for the encoding
        String detectedEncoding = detector.detect().getName();

        // Read the file using the detected encoding
        return Files.lines(filePath, Charset.forName(detectedEncoding))
                    .skip(1)
                    .map(line -> line.split("\t")[0])
                    .collect(Collectors.toList());
    }
}

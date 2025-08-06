package net.idea.wrapper.vega;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import jakarta.inject.Inject;
import net.idea.wrapper.ModelResultWriter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import insilico.core.model.InsilicoModel;
import insilico.core.molecule.InsilicoMolecule;
import insilico.core.model.InsilicoModelOutput;
import insilico.core.model.report.txt.ReportTXTSingle;
import insilico.core.model.runner.InsilicoModelRunnerByMolecule;
import insilico.core.model.runner.InsilicoModelWrapper;
import insilico.core.molecule.conversion.SmilesMolecule;


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

    @Option(names = {"-m", "--model"}, description = "Model key", required = true)
    String modelKey;

    @Option(names = {"-o", "--output"}, description = "Output report folder", required = true)
    File outputDir;

    @Option(names = {"-f", "--fastmode"},
            description = "Enable fast mode (default: false)")
    boolean fastmode = false;

    @Option(names = {"--list-models"}, description = "List available models", help = true)
    boolean listModels;
        
    @Override
    public Integer call() throws Exception {
        try {
            if (listModels) {
                
                return 0;
            } else {
                if (!outputDir.exists()) {
                    if (!outputDir.mkdirs()) {
                        throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
                    }
                } else if (!outputDir.isDirectory()) {
                    throw new IllegalArgumentException("Provided output path is not a directory: " + outputDir.getAbsolutePath());
                }

                ArrayList<InsilicoMolecule> dataset = new ArrayList<InsilicoMolecule>();
                InsilicoMolecule mol = SmilesMolecule.Convert(inputGroup.smiles.trim());
                dataset.add(mol);

                String classname = "insilico.bcf_meylan.ismBCFMeylan";
                InsilicoModel model = (InsilicoModel) Class.forName(classname).getDeclaredConstructor().newInstance();
                if (fastmode)
                    return run_fast(model , dataset, outputDir);            
                else
                    return run_vega(model , dataset, outputDir);            
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

    private int run_fast( InsilicoModel model, ArrayList<InsilicoMolecule> dataset, File outputDir) throws Exception {
        ModelResultWriter resultWriter = new ModelResultWriter(outputDir);

        for (InsilicoMolecule mol : dataset) {
            InsilicoModelOutput output = model.Execute(mol);
                
            // Prepare JSON record for this molecule and model
            Map<String, Object> record = new HashMap<>();
            record.put("smiles", output.getMoleculeSMILES());
            record.put("id", output.getMoleculeId());
            record.put("model", model.getInfo().getKey());
            record.put("assessment", output.getAssessment());
            String[] results = model.GetResultsName();            
            for(int nCols = 0; nCols < output.getResults().length; ++nCols) {
                  record.put(results[nCols],  output.getResults()[nCols]);
               }            
             /*
            for (String var : model.GetADItemsName())    
                record.put(var, model.getResultsAsMap());

            record.put("results", model.getResultsAsMap()); // assuming a method like this
                */  
            resultWriter.writeResult(model.getInfo().getKey(), record);
        }
        resultWriter.close();
        return 0;
    }
}

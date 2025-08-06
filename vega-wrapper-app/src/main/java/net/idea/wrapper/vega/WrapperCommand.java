package net.idea.wrapper.vega;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import jakarta.inject.Inject;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
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

    @Option(names = {"-s", "--smiles"}, description = "Input parameter", required = true)
    String smiles;

    @Override
    public Integer call() throws Exception {
        // Replace with your actual class/method names
        try {
            InsilicoModel implementation = (InsilicoModel) Class.forName("insilico.bcf_meylan.ismBCFMeylan").getDeclaredConstructor().newInstance();

            // Object instance = clazz.getDeclaredConstructor().newInstance();
            // Method method = clazz.getMethod("compute", String.class);
            // Object result = method.invoke(instance, input);
            ArrayList<InsilicoMolecule> DataSet = new ArrayList<InsilicoMolecule>();
            InsilicoMolecule mol = null;
            try { 
                mol = SmilesMolecule.Convert(smiles.trim());
                DataSet.add(mol);
                InsilicoModelRunnerByMolecule runner = new InsilicoModelRunnerByMolecule();
                runner.AddModel(implementation);
                runner.Run(DataSet);
                for (InsilicoModelWrapper curModel : runner.GetModelWrappers()) {
                    ReportTXTSingle.PrintReport(DataSet, curModel, new PrintWriter("report_BCF_MEYLAN.txt"));
                }

            } catch (Exception x) {
                mol = null;
                
            }                
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }
}

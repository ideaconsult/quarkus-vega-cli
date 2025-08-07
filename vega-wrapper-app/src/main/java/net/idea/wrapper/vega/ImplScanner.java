package net.idea.wrapper.vega;

import insilico.core.model.InsilicoModel;
import insilico.core.model.InsilicoModelInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.File;


public class ImplScanner {

    public static void list_models(boolean classnames_only,  File outputFile) throws Exception {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages("insilico.*") // adjust to match your package(s)
                .scan();
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))                
                ) {

            var implClasses = scanResult
                    .getClassesImplementing("insilico.core.model.iInsilicoModel")
                    .loadClasses();

            if (! classnames_only)
                // Header
                out.println("Key\tClassName\tName\tTrainingSetURL\tUnits\tSummary\tClassValues\tVega\tVersion\tADItemsName\tResultsName");                    
            for (Class<?> clazz : implClasses) {
                if (! classnames_only) {
                    try {
                        InsilicoModel model = (InsilicoModel) clazz.getDeclaredConstructor().newInstance();
                        InsilicoModelInfo info = model.getInfo();

                        String line = String.join("\t",
                            nullToEmpty(info.getKey()),
                            clazz.getName(),
                            nullToEmpty(info.getName()),
                            nullToEmpty(info.getTrainingSetURL()),
                            nullToEmpty(info.getUnits()),
                            nullToEmpty(info.getSummary()),
                            nullToEmpty(info.getClassValues()),
                            nullToEmpty(info.getVega()),
                            nullToEmpty(info.getVersion()),
                            joinArray(model.GetADItemsName(), ";"),
                            joinArray(model.GetResultsName(), ";")
                        );
                        out.println(line);
                    } catch (Exception err) {
                        System.out.println(clazz.getName());
                    }
                    
                } else {
                    out.println(clazz.getName());
                }
            }
        }
    }

    private static String nullToEmpty(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private static String joinArray(String[] arr, String delimiter) {
        if (arr == null) return "";
        return String.join(delimiter, arr);
    }    
}
package net.idea.wrapper.vega;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class ImplScanner {

    public static void list_models() {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages("insilico.*") // adjust to match your package(s)
                .scan()) {

            var implClasses = scanResult
                    .getClassesImplementing("insilico.core.model.iInsilicoModel")
                    .loadClasses();

            for (Class<?> clazz : implClasses) {
                System.out.println("Found: " + clazz.getName());
            }
        }
    }
}
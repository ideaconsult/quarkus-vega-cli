package net.idea.wrapper.vega;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import insilico.core.model.InsilicoModel;

public class ModelRegistry {

    private static final Map<String, String> modelMap = new HashMap<>();

    static {
        try (InputStream is = ModelRegistry.class.getClassLoader().getResourceAsStream("/models.txt")) {
            if (is == null) {
                throw new FileNotFoundException("models.txt not found in resources");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("\t");
                    if (parts.length == 2) {
                        modelMap.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading model registry", e);
        }
    }

    public static InsilicoModel getModelByKey(String key) throws Exception {
        String className = modelMap.get(key);
        if (className == null) {
            throw new IllegalArgumentException("Unknown model key: " + key);
        }

        Class<?> cls = Class.forName(className);
        if (!InsilicoModel.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("Class " + className + " does not implement InsilicoModel");
        }

        return (InsilicoModel) cls.getDeclaredConstructor().newInstance();
    }

    public static Set<String> getAvailableModelKeys() {
        return modelMap.keySet();
    }
}

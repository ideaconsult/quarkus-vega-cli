package net.idea.wrapper.vega.parallel;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ModelFileParserTest {

    @Test
    void testParseValidModelFile(@TempDir Path tempDir) throws IOException {
        // Create a test model file with header
        Path modelFile = tempDir.resolve("models.txt");
        Files.writeString(modelFile,
                "Key\tname\tenabled\n" +
                        "MELTING_POINT\tDescription 1\tTRUE\n" +
                        "BCF\tDescription 2\tTRUE\n" +
                        "MUTAGENICITY\tDescription 3\tTRUE\n");

        List<String> models = ModelFileParser.parseModelFile(modelFile);

        assertEquals(3, models.size());
        assertEquals("MELTING_POINT", models.get(0));
        assertEquals("BCF", models.get(1));
        assertEquals("MUTAGENICITY", models.get(2));
    }

    @Test
    void testParseFileWithEmptyLines(@TempDir Path tempDir) throws IOException {
        Path modelFile = tempDir.resolve("models.txt");
        Files.writeString(modelFile,
                "Key\tname\n" +
                        "\n" +
                        "MODEL1\tDesc\n" +
                        "\n" +
                        "MODEL2\tDesc\n" +
                        "\n\n" +
                        "MODEL3\tDesc\n");

        List<String> models = ModelFileParser.parseModelFile(modelFile);

        assertEquals(3, models.size());
    }

    @Test
    void testParseFileWithSingleColumn(@TempDir Path tempDir) throws IOException {
        Path modelFile = tempDir.resolve("models.txt");
        Files.writeString(modelFile,
                "Key\n" +
                        "MODEL1\n" +
                        "MODEL2\n" +
                        "MODEL3\n");

        List<String> models = ModelFileParser.parseModelFile(modelFile);

        assertEquals(3, models.size());
        assertEquals("MODEL1", models.get(0));
    }

    @Test
    void testParseEmptyFileThrowsException(@TempDir Path tempDir) throws IOException {
        Path modelFile = tempDir.resolve("models.txt");
        Files.writeString(modelFile, "");

        assertThrows(IOException.class, () -> {
            ModelFileParser.parseModelFile(modelFile);
        });
    }

    @Test
    void testParseNonExistentFileThrowsException(@TempDir Path tempDir) {
        Path modelFile = tempDir.resolve("nonexistent.txt");

        assertThrows(IOException.class, () -> {
            ModelFileParser.parseModelFile(modelFile);
        });
    }
}

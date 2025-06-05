package ipaddrcounter;

import ipaddrcounter.exception.FileReadException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BatchedFileReader {
    private final int linesPerTask;

    public BatchedFileReader() {
        this.linesPerTask = 10_000;
    }

    public BatchedFileReader(int linesPerTask) {
        this.linesPerTask = linesPerTask;
    }

    public void readAndExecute(String filePath, Consumer<List<String>> execute) {
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath))) {
            var batch = new ArrayList<String>(linesPerTask);
            String line;
            while ((line = reader.readLine()) != null) {
                batch.add(line);
                if (batch.size() >= linesPerTask) {
                    execute.accept(batch);
                    batch = new ArrayList<>(linesPerTask);
                }
            }
            if (!batch.isEmpty()) {
                execute.accept(batch);
            }
        } catch (IOException e) {
            throw new FileReadException(e);
        }
    }
}

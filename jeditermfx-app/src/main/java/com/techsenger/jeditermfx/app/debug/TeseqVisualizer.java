package com.techsenger.jeditermfx.app.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class TeseqVisualizer {

    public List<String> apply(List<String> chunks) {
        return chunks.stream().map(e -> apply(e)).collect(Collectors.toList());
    }

    private String apply(String text) {
        try {
            var file = writeTextToTempFile(text);
            return readOutput(List.of("teseq", file.getAbsolutePath()));
        } catch (IOException e) {
            return "(!) Control sequence visualizer `teseq` is not installed (http://www.gnu.org/software/teseq/)\n"
                    + "Printing characters as is:\n\n"
                    + text;
        }
    }

    private File createTempFile() throws IOException {
        var file = File.createTempFile("jediterm-data", ".txt");
        file.deleteOnExit();
        return file;
    }

    private File writeTextToTempFile(String text) throws IOException {
        var file = createTempFile();
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
        return file;
    }

    private String readOutput(List<String> command) throws IOException {
        var process = new ProcessBuilder(command).start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        try (var stream = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
             var reader = new BufferedReader(stream)) {
            StringBuilder result = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
            return result.toString();
        }
    }
}

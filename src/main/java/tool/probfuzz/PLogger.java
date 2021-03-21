package tool.probfuzz;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class PLogger {
    private final String filename;
    public PLogger(String outputdir){
        filename = outputdir +"/log.txt";
    }

    private void print(String message) {
        try {
            Files.write(Paths.get(this.filename), (message+"\n").getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void print(String message, boolean printToConsole) {
        if (printToConsole) {
            System.out.println(message);
        }
        print(message);
    }

    public void printo(String message){
        print(message, true);
    }
}

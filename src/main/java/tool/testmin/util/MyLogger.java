package tool.testmin.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MyLogger {
    public static Map<String, MyLogger> LoggerPool = new HashMap<>();
    public String dir_suffix;
    private String filename;
    private String threadName;

    public MyLogger(TestFile fileinfo, String threadName, String dir_suffix) {
        filename = fileinfo.getTestdir() + "/log_" + dir_suffix + ".txt";
        this.threadName = threadName;
        this.dir_suffix = dir_suffix;
        LoggerPool.put(threadName, this);
    }

    private void print(String message) {
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(filename, true);
            fileWriter.write(message + "\n");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void print(String message, boolean printToConsole) {
        if (printToConsole) {
            System.out.println("[" + threadName + "] " + message);
        }

        print(message);
    }
}

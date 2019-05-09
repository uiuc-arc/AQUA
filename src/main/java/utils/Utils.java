package utils;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.Properties;

public class Utils {

    private static String CONFIGURATIONFILE ="src/main/resources/config.properties";
    public static String STANRUNNER;
    static {
    Properties properties = new Properties();
    try{
        FileInputStream fileInputStream = new FileInputStream(CONFIGURATIONFILE);
        properties.load(fileInputStream);
        STANRUNNER = properties.getProperty("stan.script");
    } catch (IOException e) {
        e.printStackTrace();
    }
    }

    public static Pair<String, String> runStan(String filename, String datafilename){
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(STANRUNNER + " " + filename + " " + datafilename);
            p.waitFor();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            reader.lines().forEach(e -> output.append(e).append("\n"));
            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            StringBuilder error = new StringBuilder();
            reader.lines().forEach(e -> error.append(e).append("\n"));

            return new ImmutablePair<>(output.toString(), error.toString());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }
}

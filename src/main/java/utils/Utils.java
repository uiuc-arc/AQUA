package utils;

import java.io.FileInputStream;
import java.io.IOException;
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

    public void runStan(String filename, String datafilename){
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(STANRUNNER + " " + filename + " " + datafilename + " " + );
            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

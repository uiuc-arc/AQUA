package tool.testmin.templatetransformers;

import tool.testmin.util.MyLogger;
import tool.testmin.util.TestFile;
import tool.testmin.util.TMUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SamplerReducer {
    public static Map<String, Integer> samplingMap;
    public static Map<String, Integer> original_samplingMap;
    public static Map<String, String> inferenceType;
    public static boolean variationalEnabled;

    private static String modifiedFile;
    private static Integer oldValue;

    static {
        samplingMap = new HashMap<>();
        original_samplingMap = new HashMap<>();
        inferenceType = new HashMap<>();
        variationalEnabled = true;

        ArrayList<TestFile> testfiles = TMUtil.getTestFiles();

        for (TestFile fileinfo : testfiles) {
            samplingMap.put(fileinfo.getTestfile(), fileinfo.getIterations());
            original_samplingMap.put(fileinfo.getTestfile(), fileinfo.getIterations());


            inferenceType.put(fileinfo.getTestfile(), fileinfo.getAlgotype());
        }

    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                if (!samplingMap.containsKey(testfile)) {
                    logger.print("No samples/iters", true);
                    return false;
                }
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int currentSamples = samplingMap.get(testfile);
                if (currentSamples < 2) {
                    logger.print("Not enough samples... : ", true);
                    return false;
                }

                while (currentSamples >= 2) {
                    samplingMap.put(testfile, currentSamples / 2);
                    logger.print("Reducing samples to : " + currentSamples / 2, true);
                    if (TMUtil.runTransformedCode(filecontent,
                            filecontent,
                            TMUtil.TemplateTransformationMap.inverse().get(SamplerReducer.class),
                            logger,
                            testFile,
                            true)) {
                        currentSamples = currentSamples / 2;
                        changed = true;
                    } else {
                        samplingMap.put(testfile, currentSamples);
                        break;
                    }
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return changed;
    }

}


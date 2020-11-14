package grammar.analyses;

import grammar.cfg.CFGBuilder;
import grammar.cfg.Section;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.custom.CumSum;
import org.nd4j.linalg.factory.NDArrayFactory;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.indexing.conditions.Conditions;
import translators.Stan2IRTranslator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class AnalysisRunner {

    public static void analyzeProgram (String localDir, String stanPath) {
        int index0=stanPath.lastIndexOf('/');
        String stanName = stanPath.substring(index0+1,stanPath.length());
        String stanfile = localDir + stanPath + "/" + stanName + ".stan";
        String standata = localDir + stanPath + "/" + stanName + ".data.R";
        // String stansummary = localDir + stanPath + "/" + StringUtils.substringBefore(stanName, "_robust") + "_rw_summary_1000.txt";
        String stansummary = localDir + stanPath + "/" + stanName + "_rw_summary_1000.txt";
        int index=stanfile.lastIndexOf('/');
        String filePath = stanfile.substring(0,index);
        Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator(stanfile, standata);
        String tempFileName = stanfile.replace(".stan", "");
        String templateCode = stan2IRTranslator.getCode();
        System.out.println("========Stan Code to Template=======");
        System.out.println(templateCode);
        File tempfile = null;
        try {
            tempfile = File.createTempFile(tempFileName, ".template");
            FileUtils.writeStringToFile(tempfile, templateCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long startTime = System.nanoTime();
        CFGBuilder cfgBuilder = new CFGBuilder(tempfile.getAbsolutePath(), null);
        ArrayList<Section> CFG = cfgBuilder.getSections();
        IntervalAnalysis intervalAnalyzer = new IntervalAnalysis();
        intervalAnalyzer.setPath(filePath);
        intervalAnalyzer.setSummaryFile(stansummary);
        intervalAnalyzer.forwardAnalysis(CFG);
        double[] avgMetrics = FindMetrics(filePath);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1000000000.0;
        System.out.println("Analysis Time: " + duration);
        System.out.println("Total Variation Distance:" + avgMetrics[0]);
        System.out.println("K-S Distance:" + avgMetrics[1]);
    }


    public static double[] FindMetrics(String path) {
        double avgTVD = 0;
        double avgKS = 0;
        int count = 0;
        File dir = new File(path);
        File fileList[] = dir.listFiles();
        for (File file : fileList) {
            String fileName = file.getName();
            if (fileName.endsWith(".txt") && fileName.startsWith("analysis")
                    && (!fileName.contains("robust_"))) {
                INDArray currParam = Nd4j.readTxt(file.getPath());
                INDArray nanMatrix = currParam.dup();
                BooleanIndexing.replaceWhere(nanMatrix,0, Conditions.equals(Double.NaN));
                BooleanIndexing.replaceWhere(nanMatrix,1, Conditions.isNan());
                if (nanMatrix.sum().getDouble() > 0) {
                    System.out.println(fileName + " contains NaN");
                }
                count += 1;
                double[] ret = TVD_KS(currParam);
                // System.out.println(fileName);
                // System.out.println(String.format("TVD: %s, KS: %s", ret[0], ret[1]));
                avgTVD += ret[0];
                avgKS += ret[1];
            }
        }
        return new double[]{avgTVD/(double) count, avgKS/(double) count};
    }

    public static double[] TVD_KS(INDArray param) {
        double TVDret = 0;
        double KSret = 0;
        double[] value = param.slice(0).toDoubleVector();
        double[] probl = Nd4j.cumsum(param.slice(1)).toDoubleVector();
        double[] probu = Nd4j.cumsum(param.slice(2)).toDoubleVector();
        double[] probll =  new double[probl.length + 1];
        System.arraycopy(probl, 0, probll, 1, probl.length);
        probll[0] = 0;
        double[] problu =  new double[probl.length + 1];
        System.arraycopy(probl, 0, problu, 0, probl.length);
        problu[probl.length] = 1;
        double[] probul =  new double[probl.length + 1];
        System.arraycopy(probu, 0, probul, 1, probu.length);
        probul[0] = 0;
        double[] probuu =  new double[probl.length + 1];
        System.arraycopy(probu, 0, probuu, 0, probl.length);
        probuu[probu.length] = 1;
        for (int i = 0; i < probl.length - 1; i++) {
            double rectHeight = Math.max(Math.abs(problu[i] - probul[i]), Math.abs(probuu[i] - probll[i]));
            double rectWidth = value[i + 1] - value[i];
            TVDret += rectHeight * rectWidth;
            // System.out.println(String.format("%s,%s,%s",rectHeight * rectWidth, rectHeight, rectWidth));
            KSret = Math.max(KSret, rectHeight);
        }
        return new double[]{TVDret / 2.0, KSret};
    }


    public static void main (String[] args) {
        String localDir = "/home/zixinh2/analysis/progs/";
        // String localDir = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/";
        AnalysisRunner.analyzeProgram(localDir, args[0]);
    }
}

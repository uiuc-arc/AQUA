package grammar.analyses;

import grammar.cfg.CFGBuilder;
import grammar.cfg.Section;
import org.apache.commons.io.FileUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import translators.Stan2IRTranslator;

import java.io.*;
import java.util.*;

public class AnalysisRunner {
    private static List<String> hierModels = Arrays.asList("anova_radon_nopred_chr", "anova_radon_nopred", "electric_chr", "hiv", "pilots", "radon.1", "radon_no_pool_chr", "radon_no_pool", "radon_vary_inter_slope_17.1", "radon_vary_si_chr", "radon_vary_si");

    public static void analyzeProgram (String localDir, String stanPath) {
        int index0=stanPath.lastIndexOf('/');
        String stanName = stanPath.substring(index0+1,stanPath.length());
        String stanfile = localDir + stanPath + "/" + stanName + ".stan";
        String standata = localDir + stanPath + "/" + stanName + ".data.R";
        if (hierModels.contains(stanName.split("_robust")[0])) {
            standata = localDir + stanPath + "/" + "one" + ".data.R";
            System.out.println("remove prior!!!");
        }
        // String stansummary = localDir + stanPath + "/" + StringUtils.substringBefore(stanName, "_robust") + "_rw_summary_1000.txt";
        String stansummary = localDir + stanPath + "/" +  "rw_summary_100";
        // String stansummary = localDir + stanPath + "/" + stanName + "_rw_summary_100.txt";
        Map<String, String> TruthSummary = getMeanFromTruth(localDir + stanPath + "/" + "truth_file_w");
        int index=stanfile.lastIndexOf('/');
        String filePath = stanfile.substring(0,index);

        Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator(stanfile, standata);
        String tempFileName = stanfile.replace(".stan", "");
        String templateCode = stan2IRTranslator.getCode();
        // System.out.println("========Stan Code to Template=======");
        // System.out.println(templateCode);
        File tempfile = null;
        try {
            tempfile = File.createTempFile(tempFileName, ".template");
            FileUtils.writeStringToFile(tempfile, templateCode);
        } catch (IOException e) {
            e.printStackTrace();
        }


        long startTime = System.nanoTime();
        // /*
        CFGBuilder cfgBuilder = new CFGBuilder(tempfile.getAbsolutePath(), null);
        ArrayList<Section> CFG = cfgBuilder.getSections();
        IntervalAnalysis intervalAnalyzer = new IntervalAnalysis();
        // if (hierModels.contains(stanName)) {
        //     intervalAnalyzer.no_prior = true;
        // } else {
        //     intervalAnalyzer.no_prior = false;
        // }
        intervalAnalyzer.setPath(filePath);
        intervalAnalyzer.setSummaryFile(stansummary);
        intervalAnalyzer.forwardAnalysis(CFG);
        // */
        //===========Find Metrics================
        String outputName = "/output0127.txt";
        double[] avgMetrics = FindMetrics(filePath, TruthSummary, outputName);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1000000000.0;


        System.out.println("Analysis Time: " + duration);
        System.out.println("Total Variation Distance:" + avgMetrics[0]);
        System.out.println("K-S Distance:" + avgMetrics[1]);
        System.out.println("Exp Change:" + avgMetrics[2]);
        System.out.println("MSE Change:" + avgMetrics[3]);
        System.out.println("True Change:" + avgMetrics[4]);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(filePath + outputName,true),true);
            writer.println( "avg," +Arrays.toString(avgMetrics).replace("[","").replace("]","").replace(" ","") ); // String.valueOf(duration) + "," +
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // if the original is best, remove the first & last block to try again
    public static double[] FindMetrics(String filePath, Map<String, String> TruthSummary, String outputName) {
        double avgTVD = 0;
        double avgKS = 0;
        double avgExp = 0;
        double avgMSE = 0;
        double avgTrue = 0;
        int count = 0;
        int countT = 0;
        File dir = new File(filePath);
        File fileList[] = dir.listFiles();
        try {
            PrintWriter writer = null;
            writer = new PrintWriter(new FileWriter(filePath + outputName),true);
            for (File file : fileList) {
                String fileName = file.getName();
                if (fileName.endsWith(".txt") && fileName.startsWith("analysis")
                        && (!fileName.contains("robust_"))) {
                    INDArray currParam = Nd4j.readTxt(file.getPath());
                    // INDArray nanMatrix = currParam.dup();
                    // BooleanIndexing.replaceWhere(nanMatrix,0, Conditions.equals(Double.NaN));
                    // BooleanIndexing.replaceWhere(nanMatrix,1, Conditions.isNan());
                    // if (nanMatrix.sum().getDouble() > 1) {
                    //     System.out.println(fileName + " contains NaN");
                    // }
                    count += 1;
                    double[] ret = TVD_KS(currParam);
                    // System.out.println(fileName);
                    // System.out.println(String.format("TVD: %s, KS: %s", ret[0], ret[1]));
                    avgTVD += ret[0];
                    avgKS += ret[1];
                    String paramName = fileName.replace("analysis_","").replace(".txt","");
                    if (TruthSummary.containsKey(paramName)) { // && !paramName.contains("sigma") && !paramName.contains("sigma[2]") ) {
                        double expDist = MSE(currParam);
                        double truthDist = MSETruth(currParam, Double.valueOf(TruthSummary.get(paramName)));
                        avgExp += expDist;
                        avgMSE += Math.pow(expDist, 2);
                        avgTrue += truthDist;
                        countT += 1;
                        System.out.println(paramName);
                        System.out.println(truthDist);
                        double[] paramMetrics = new double[]{ret[0],ret[1],expDist,Math.pow(expDist,2),truthDist};
                        writer.println( paramName + "," +Arrays.toString(paramMetrics).replace("[","").replace("]","").replace(" ","")); // String.valueOf(duration) + "," +
                    }
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new double[]{avgTVD/(double) count, avgKS/(double) count, avgExp/(double) countT, avgMSE/(double) countT, avgTrue/(double) countT};
    }

    public static double MSE(INDArray param) {
        double[] value = param.slice(0).toDoubleVector();
        double[] pdfl = param.slice(1).toDoubleVector();
        double[] pdfu = param.slice(2).toDoubleVector();
        return Math.abs(getE(value, pdfl) - getE(value, pdfu));
    }


    public static double MSETruth(INDArray param, Double param_truth) {
        double[] value = param.slice(0).toDoubleVector();
        double[] pdfl = param.slice(1).toDoubleVector();
        double[] pdfu = param.slice(2).toDoubleVector();
        return Math.pow(getE(value, pdfl) - param_truth,2);
    }


    private static Map<String, String> getMeanFromTruth(String TruthSummary) {
        Map<String, String> records = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(TruthSummary))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;
                String[] values = line.split(",");
                if (values[0].contains("__"))
                    continue;
                records.put(values[0].replace("\"", ""), values[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return records;
    }



    public static double getE(double[] value, double[] prob) {
        double expectation = 0;
        for (int i =0; i< value.length; i++) {
            expectation += value[i] * prob[i];
        }
        return expectation;
    }

    public static double[] TVD_KS(INDArray param) {
        double TVDret = 0;
        double KSret = 0;
        double[] value = param.slice(0).toDoubleVector();
        INDArray pdfl = param.slice(1).get(NDArrayIndex.interval(0,value.length ));
        INDArray pdfu = param.slice(2).get(NDArrayIndex.interval(0,value.length ));
        double[] pdflo = pdfl.toDoubleVector();
        double[] pdfuo = pdfu.toDoubleVector();
        if (pdfl.sumNumber().doubleValue() != 0)
            pdfl = pdfl.div(pdfl.sumNumber());
        else
            pdfl = Nd4j.ones(pdfl.shape());
        if (pdfu.sumNumber().doubleValue() != 0)
            pdfu = pdfu.div(pdfu.sumNumber());
        else
            pdfu = Nd4j.ones(pdfu.shape());
        double[] probl = Nd4j.cumsum(pdfl).toDoubleVector();
        double[] probu = Nd4j.cumsum(pdfu).toDoubleVector();
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
            // TVD on CDF
            // double rectHeight = Math.max(Math.abs(problu[i] - probul[i]), Math.abs(probuu[i] - probll[i]));
            // TVD on PDF
            double rectHeight = Math.abs(pdflo[i] - pdfuo[i]);
            double cdfHeight = Math.abs(probl[i] - probu[i]);
            double rectWidth = value[i + 1] - value[i];
            TVDret += rectHeight * rectWidth;
            // System.out.println(String.format("%s,%s,%s",rectHeight * rectWidth, rectHeight, rectWidth));
            KSret = Math.max(KSret, cdfHeight);
        }
        return new double[]{TVDret / 2.0, KSret};
    }


    public static void main (String[] args) {
        // String localDir = "/home/zixin/analysis_progs/progs/";
        String localDir = "/Users/zixin/Documents/uiuc/fall20/analysis/analysis_progs/progs/all/";
        AnalysisRunner.analyzeProgram(localDir, args[0]);
    }
}

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
        // Map<String, String> TruthSummary = getMeanFromTruth(localDir + stanPath + "/" + "truth_file_w");
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


        anaTempFile(localDir, stansummary, filePath, tempfile.getAbsolutePath());





        /*
        double[] avgMetrics = FindMetrics(filePath, TruthSummary, outputName);
        System.out.println("MSE Change:" + avgMetrics[3]);
        System.out.println("True Change:" + avgMetrics[4]);
        System.out.println("MSE Change:" + avgMetrics[8]);
        System.out.println("True Change:" + avgMetrics[9]);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(filePath + outputName,true),true);
            writer.println( "avg," +Arrays.toString(Arrays.copyOfRange(avgMetrics,0,5)).replace("[","").replace("]","").replace(" ","") + "," + String.valueOf(duration)); //  + "," +
            writer.println( "avgs," +Arrays.toString(Arrays.copyOfRange(avgMetrics, 5, 10)).replace("[","").replace("]","").replace(" ","") + "," + String.valueOf(duration));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }
    public static void analyzeTemplate (String localDir, String tempfilePath) {
        String filePath = localDir + tempfilePath.substring(0, tempfilePath.length() - 9);
        anaTempFile(localDir, null, filePath, filePath + "/" + tempfilePath);
    }

    private static void anaTempFile(String localDir, String stansummary, String filePath, String tempfilePath) {
        // /*
        long startTime = System.nanoTime();
        CFGBuilder cfgBuilder = new CFGBuilder(tempfilePath, null, true);
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
        //===========Find Metrics================
        // */
        String outputName = "/output0202.txt";
        try {
            Process p = Runtime.getRuntime().exec(localDir.replaceAll("progs/.../","") + "integrate.py " + filePath);
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String s;
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
            stdError = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1000000000.0;
        System.out.println("Analysis Time: " + filePath + "," + duration);
    }


    // if the original is best, remove the first & last block to try again
    public static double[] FindMetrics(String filePath, Map<String, String> TruthSummary, String outputName) {
        double avgTVD = 0;
        double avgKS = 0;
        double avgExp = 0;
        double avgMSE = 0;
        double avgTrue = 0;
        int count = 0;
        double avgTVDs = 0;
        double avgKSs = 0;
        double avgExps = 0;
        double avgMSEs = 0;
        double avgTrues = 0;
        int counts = 0;
        File dir = new File(filePath);
        File fileList[] = dir.listFiles();
        try {
            PrintWriter writer = null;
            writer = new PrintWriter(new FileWriter(filePath + outputName),true);
            for (File file : fileList) {
                String fileName = file.getName();
                String paramName = fileName.replace("analysis_","").replace(".txt","");
                if (fileName.endsWith(".txt") && fileName.startsWith("analysis")
                        && (!fileName.contains("robust_"))
                        && TruthSummary.containsKey(paramName)) {
                    INDArray currParam = Nd4j.readTxt(file.getPath());
                    count += 1;
                    double[] ret = TVD_KS(currParam);
                    double expDist = MSE(currParam);
                    double truthDist = MSETruth(currParam, Double.valueOf(TruthSummary.get(paramName)));
                    avgTVD += ret[0];
                    avgKS += ret[1];
                    avgExp += expDist;
                    avgMSE += Math.pow(expDist, 2);
                    avgTrue += truthDist;
                    if (!paramName.contains("sigma")) {
                        avgTVDs += ret[0];
                        avgKSs += ret[1];
                        avgExps += expDist;
                        avgMSEs += Math.pow(expDist, 2);
                        avgTrues += truthDist;
                        counts +=1;

                    }
                    double[] paramMetrics = new double[]{ret[0],ret[1],expDist,Math.pow(expDist,2),truthDist};
                    writer.println( paramName + "," +Arrays.toString(paramMetrics).replace("[","").replace("]","").replace(" ",""));
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new double[]{avgTVD/(double) count, avgKS/(double) count, avgExp/(double) count, avgMSE/(double) count, avgTrue/(double) count,
                            avgTVDs/(double) counts, avgKSs/(double) counts, avgExps/(double) counts, avgMSEs/(double) counts, avgTrues/(double) counts};
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
                if (line.equals(""))
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
        double sump = 0;
        for (int i =0; i< value.length; i++) {
            expectation += value[i] * prob[i];
            sump += prob[i];
        }
        System.out.println((expectation));
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
        String localDir = "/home/zixin/Documents/are/analysis_progs/progs/all/";
        String localDirPSI = "/home/zixin/Documents/are/analysis_progs/progs/psi/";
        // String localDir = "/Users/zixin/Documents/uiuc/fall20/analysis/analysis_progs/progs/all/";
        if (args[0].endsWith("template")) {
            System.out.println(localDirPSI + args[0]);
            AnalysisRunner.analyzeTemplate(localDirPSI, args[0]);
        }
        else {
            System.out.println(localDir + args[0]);
            AnalysisRunner.analyzeProgram(localDir, args[0]);
        }
    }
}

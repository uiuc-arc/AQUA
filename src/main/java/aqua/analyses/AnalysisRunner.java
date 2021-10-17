package aqua.analyses;

import aqua.cfg.CFGBuilder;
import grammar.cfg.Section;
import org.apache.commons.io.FileUtils;
import translators.Stan2IRTranslator;

import javax.json.*;
import java.io.*;
import java.util.*;

public class AnalysisRunner {

    // For the following programs with infinite support,
    // AQUA applies an adaptive algorithm for selecting the number of splits


    public static void analyzeProgram (String localDir, String stanPath, String splits) throws IOException {
        int index0=stanPath.lastIndexOf('/');
        String stanName = stanPath.substring(index0+1,stanPath.length());
        String stanfile = localDir + stanPath + "/" + stanName + ".stan";
        String standata = localDir + stanPath + "/" + stanName + ".data.R";
        String stansummary = localDir + stanPath + "/" +  "rw_summary_100";
        // String stansummary = localDir + stanPath + "/" + stanName + "_rw_summary_100.txt";
        // Map<String, String> TruthSummary = getMeanFromTruth(localDir + stanPath + "/" + "truth_file_w");
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


        anaTempFile(localDir, stansummary, filePath, tempfile.getAbsolutePath(), splits);

    }
    public static void analyzeTemplate (String localDir, String tempfilePath, String splits) throws IOException {
        int index=tempfilePath.lastIndexOf('/');
        String filePath = tempfilePath.substring(0,index);
        anaTempFile(localDir, null, filePath, tempfilePath, splits);
    }

    // Method for running analysis
    private static void anaTempFile(String localDir, String stansummary, String filePath, String tempfilePath, String splits) throws IOException {

        // Check if the program has infinite support in order to use the adaptive algorithm
        FileInputStream fis = new FileInputStream("benchmark_list.json");
        JsonReader rdr = Json.createReaderFactory(null).createReader(fis, java.nio.charset.StandardCharsets.UTF_8);
        JsonObject obj = rdr.readObject();
        List<String> finiteModels = new ArrayList<>();
        JsonArray fModels = obj.getJsonArray("finite_models");
        for (JsonValue fModel : fModels) {
            String fm = fModel.toString();
            finiteModels.add(fm.substring(1, fm.length() - 1));
        }
        String[] filePathSplit = filePath.split("/");
        boolean inf_cont = !finiteModels.contains(filePathSplit[filePathSplit.length - 1]);

        // Construct CFG
        long startTime = System.nanoTime();
        long endTime1 = startTime;
        aqua.cfg.CFGBuilder cfgBuilder = new CFGBuilder(tempfilePath, null, false);
        ArrayList<Section> CFG = cfgBuilder.getSections();
        IntervalAnalysis intervalAnalyzer = new IntervalAnalysis();
        intervalAnalyzer.setPath(filePath);
        intervalAnalyzer.setSummaryFile(stansummary);

        // Start analysis
        Integer intSplits = 61;

        if (inf_cont)
            intervalAnalyzer.maxCounts = 11;
        else
            intervalAnalyzer.maxCounts = intSplits;
        intervalAnalyzer.forwardAnalysis(CFG);

        // Adapt the intervals and repeat the analysis
        // if using the adaptive algorithm
        if (inf_cont) {
            boolean repeat = intervalAnalyzer.getNewRange();
            while (repeat) {
                intervalAnalyzer.repeatAna();
                repeat = intervalAnalyzer.getNewRange();
            }
            intervalAnalyzer.maxCounts = intSplits;
            intervalAnalyzer.repeatAna();
        }

        // One can also use python to compute marginalized posterior
        // from the unnormalized joint density
        //callPython(localDir, filePath, String.valueOf(intSplits));


        // Write Timing Results
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1000000000.0;

        System.out.println("Analysis Time (s): " + filePath + "," + duration ); // + "," + String.valueOf(intervalAnalyzer.maxCounts - 1)
    }

    private static void callPython(String localDir, String filePath, String splits) {
        try {
            Process p = Runtime.getRuntime().exec(localDir.replaceAll("progs/.../","") + "integrate.py " + filePath + " " + splits);
            p.waitFor();
            // BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            // String s;
            // while ((s = stdError.readLine()) != null) {
            //     System.out.println(s);
            // }
            // stdError = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // while ((s = stdError.readLine()) != null) {
            //     System.out.println(s);
            // }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void main (String[] args) throws IOException {

        if (args[0].endsWith("template")) {
            String localDirPSI = "";
            System.out.println(localDirPSI + args[0]);
            AnalysisRunner.analyzeTemplate(localDirPSI, args[0], "61");
        }
        else {
            String localDir = "";
            System.out.println(localDir + args[0]);
            AnalysisRunner.analyzeProgram(localDir, args[0], "61"); // args[1]
        }
    }

    /*
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
    */


}

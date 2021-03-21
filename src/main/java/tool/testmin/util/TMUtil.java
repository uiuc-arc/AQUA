package tool.testmin.util;

import com.google.common.collect.HashBiMap;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.unix4j.Unix4j;
import grammar.StanLexer;
import grammar.StanParser;
import grammar.Python3Lexer;
import grammar.Python3Parser;
import grammar.Template2Lexer;
import grammar.Template2Parser;
import tool.testmin.TestMinimizer;
import tool.testmin.templatetransformers.DistributionNormalizer;
import tool.testmin.util.template.TemplateDimensionChecker;
import tool.testmin.util.template.TemplateDimensionEvaluator;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class TMUtil {

    public static Map<String, String> TransformationSequence;
    public static int MAX_THREADS;
    public static boolean RUNBASICONLY;
    public static String DistributionChoice;
    public static String TimingFile;
    public static boolean RunCov;
    public static HashBiMap<Integer, Class> TemplateTransformationMap;
    public static Map<String, Map<Integer, Integer[]>> Transformations;
    public static Map<String, Long> TimingMap;
    public static Map<String, Long> CompileTime;
    public static ArrayList<Integer> order;
    public static Map<String, Map<Integer, Integer>> reductionMap;
    public static Map<String, List<String>> distReplacementMap;
    private static String DISTRIBUTIONFILE;
    private static String FUNCTIONSFILE;
    private static String TESTFILES;
    private static Map<String, ArrayList<Integer>> orders;

    static {

        reductionMap = new HashMap<>();
        Transformations = new HashMap<>();
        TimingMap = new HashMap<>();

        CompileTime = new HashMap<>();
        TransformationSequence = new HashMap<>();
        distReplacementMap = new HashMap<>();
        orders = new HashMap<>();
        // order 1: normal
        order = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
        orders.put("static", order);
        order = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
        Collections.shuffle(order);
        orders.put("random", order);

        // order 2: size of transformation
        order = new ArrayList<Integer>(Arrays.asList(5, 6, 4, 15, 2, 3, 9, 0, 1, 10, 12, 14, 11, 13, 7, 8));
        orders.put("size", order);

        // order 3: cost of analysis
        order = new ArrayList<>(Arrays.asList(5, 6, 4, 12, 1, 14, 10, 15, 0, 9, 3, 2, 11, 7, 8, 13));
        orders.put("analysis", order);

        // order 4: random
        //Collections.shuffle(order);
        // order 5: basic then domain
        order = new ArrayList<Integer>(Arrays.asList(5, 6, 4, 1, 14, 15, 2, 7, 8, 12, 10, 0, 9, 3, 11, 13));
        orders.put("basic", order);
        // order 6 : domain then basic
        order = new ArrayList<Integer>(Arrays.asList(12, 10, 0, 9, 3, 11, 13, 5, 6, 4, 1, 14, 15, 2, 7, 8));
        orders.put("domain", order);
        //assert order!=null && order.size() == TemplateTransformationMap.keySet().size();

        //System.out.println("Order : "+order);

    }

    static {
        Properties properties = new Properties();
        try {
            String CONFIGURATIONFILE = "src/main/resources/config.properties";
            FileInputStream fileInputStream = new FileInputStream(CONFIGURATIONFILE);
            properties.load(fileInputStream);
            properties.getProperty("MAX_THREADS");
            MAX_THREADS = Integer.parseInt(properties.getProperty("MAX_THREADS"));
            DISTRIBUTIONFILE = properties.getProperty("distributionFile");
            FUNCTIONSFILE = properties.getProperty("functionsFile");
            TESTFILES = String.format("src/main/resources/testfiles_%s.json", properties.getProperty("TESTSET"));
            RUNBASICONLY = Boolean.parseBoolean(properties.getProperty("BASIC"));
            DistributionChoice = properties.getProperty("DistributionChoice");
            TimingFile = "src/main/resources/t.csv";
            RunCov = Boolean.parseBoolean(properties.getProperty("RUNCOV"));
            String ORDER = properties.getProperty("ORDER");
            try {
                order = orders.get(ORDER);
            } catch (Exception e) {
                System.out.println("Illegal Order");
                order = orders.get("static");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    static {
        TemplateTransformationMap = HashBiMap.create();
        TemplateTransformationMap.put(0, tool.testmin.templatetransformers.LimitsRemover.class);
        TemplateTransformationMap.put(1, tool.testmin.templatetransformers.LoopVarRemover.class);
        TemplateTransformationMap.put(2, tool.testmin.templatetransformers.ArithEliminator.class);

        TemplateTransformationMap.put(3, tool.testmin.templatetransformers.FunctionCallEliminator.class);  //domain
        TemplateTransformationMap.put(4, tool.testmin.templatetransformers.FunctionCallStmtEliminator.class);
        TemplateTransformationMap.put(5, tool.testmin.templatetransformers.LoopEliminator.class);
        TemplateTransformationMap.put(6, tool.testmin.templatetransformers.ConditionalEliminator.class);
        TemplateTransformationMap.put(7, tool.testmin.templatetransformers.AssignmentRemover.class);
        TemplateTransformationMap.put(8, tool.testmin.templatetransformers.SamplingRemover.class);
        TemplateTransformationMap.put(9, DistributionNormalizer.class);  //domain
        TemplateTransformationMap.put(10, tool.testmin.templatetransformers.ParameterEliminator.class);   //domain
        TemplateTransformationMap.put(11, tool.testmin.templatetransformers.DataTransformer.class);  //domain
        TemplateTransformationMap.put(12, tool.testmin.templatetransformers.SamplerReducer.class);   //domain
//        TemplateTransformationMap.put(13, DistributionSubstitute.class);   //domain
        TemplateTransformationMap.put(14, tool.testmin.templatetransformers.UnusedItemRemover.class);
        TemplateTransformationMap.put(15, tool.testmin.templatetransformers.Slicer.class);


        // order 1: normal
//        order = new ArrayList<Integer>(Arrays.asList(new Integer[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}));
        //Collections.shuffle(order);

        // order 2: size of transformation
        //order = new ArrayList<Integer>(Arrays.asList(new Integer[]{5, 6, 4, 15, 2, 3, 9, 0, 1, 10, 12, 14, 11, 13, 7 ,8}));

        // order 3: cost of analysis
        // order = new ArrayList<>(Arrays.asList(new Integer[]{5, 6, 4, 12, 1, 14, 10, 15, 0, 9, 3, 2, 11, 7, 8, 13}));

        // order 4: random
        //Collections.shuffle(order);
        // order 5: basic then domain
        //order = new ArrayList<Integer>(Arrays.asList(new Integer[]{5,6,4,1,14,15,2,7,8,12, 10, 0, 9, 3, 11, 13 }));
        // order 6 : domain then basic
        //order = new ArrayList<Integer>(Arrays.asList(new Integer[]{12, 10, 0, 9, 3, 11, 13, 5,6,4,1,14,15,2,7,8}));
        //       assert order!=null && order.size() == TemplateTransformationMap.keySet().size();

        //       System.out.println("Order : "+order);

    }

    public static List<JsonObject> getDistributions(String pps) {
        FileInputStream fis;
        List<JsonObject> models = null;
        try {
            fis = new FileInputStream(DISTRIBUTIONFILE);
            JsonReader jsonReader = Json.createReader(fis);
            JsonObject jsonObject = jsonReader.readObject();
            models = new ArrayList<>();
            List<JsonObject> dists = jsonObject.getJsonArray("models").getValuesAs(JsonObject.class);
            for (JsonObject j : dists) {
                if (pps != null && !pps.isEmpty()) {
                    if (j.containsKey(pps)) {
                        models.add(j);
                    }
                } else {
                    models.add(j);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return models;
    }

    public static boolean checkIfReplaced(String filename, Template2Parser.DistexprContext distexprContext) {
        if (!distReplacementMap.containsKey(filename)) {
            distReplacementMap.put(filename, new ArrayList<>());
        }
        List<String> dists = distReplacementMap.get(filename);
        if (dists.contains(distexprContext.getText().trim())) {
            return true;
        } else {
            dists.add(distexprContext.getText().trim());
            return false;
        }
    }

    public static boolean isPrimitive(ParserRuleContext ctx, String program) {
        Dimension dimension = new TemplateDimensionEvaluator(TemplateDimensionChecker.getDimensionMap(program)).visit(ctx);
        return dimension != null && dimension.isPrimitive();
    }

    public static void removeReplacedDist(String filename, String dist) {
        if (dist == null) {
            System.out.println("Dist is null");
            return;
        }
        if (distReplacementMap.containsKey(filename)) {
            List<String> dists = distReplacementMap.get(filename);
            if (dists == null) {
                System.out.println("Dist replacement map not found: " + filename);
                return;
            }

            dists.remove(dist);
        }
    }

    public static void updateReductionCount(String testfile, int transformation, String oldContent, String newContent) {
        if (!reductionMap.containsKey(testfile)) {
            reductionMap.put(testfile, new HashMap<>());
        }

        Map<Integer, Integer> map = reductionMap.get(testfile);
        if (!map.containsKey(transformation)) {
            map.put(transformation, 0);
        }

        int oldcount = 0, newcount = 0;
        oldcount = getPartsCount(oldContent);
        newcount = getPartsCount(newContent);

//        if(testfile.endsWith("template")){
//            oldcount = TemplatePartsCounter.CountFromString(oldContent);
//            newcount = TemplatePartsCounter.CountFromString(newContent);
//
//        }
//        else{
//            oldcount = PartsCounter.CountFromString(oldContent);
//            newcount = PartsCounter.CountFromString(newContent);
        //}
        map.put(transformation, map.get(transformation) + (oldcount - newcount));
    }


    public static StanParser getParser(String filename) {
        FileInputStream is = null;
        try {
            is = new FileInputStream(filename);
            //ANTLRInputStream antlrInputStream = new ANTLRInputStream(is);
            StanLexer stanLexer = new StanLexer(CharStreams.fromFileName(filename));
            CommonTokenStream tokens = new CommonTokenStream(stanLexer);
            StanParser stanParser = new StanParser(tokens);
            return stanParser;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Python3Parser getPythonParser(String filename) {
        FileInputStream is = null;
        try {
            is = new FileInputStream(filename);
            //ANTLRInputStream antlrInputStream = new ANTLRInputStream(is);
            StanLexer stanLexer = new StanLexer(CharStreams.fromFileName(filename));
            CommonTokenStream tokens = new CommonTokenStream(stanLexer);
            Python3Parser Python3Parser = new Python3Parser(tokens);
            return Python3Parser;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Template2Parser getTemplateParser(String filename) {
        FileInputStream is = null;
        try {
            is = new FileInputStream(filename);
            Template2Lexer template2Lexer = new Template2Lexer(CharStreams.fromFileName(filename));
            CommonTokenStream tokens = new CommonTokenStream(template2Lexer);
            Template2Parser parser = new Template2Parser(tokens);
            return parser;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Python3Parser getPython3ParserFromString(String file) {
        try {
            Python3Lexer stanLexer = new Python3Lexer(CharStreams.fromString(file));
            CommonTokenStream tokens = new CommonTokenStream(stanLexer);
            Python3Parser stanParser = new Python3Parser(tokens);
            return stanParser;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static StanParser getParserFromString(String file) {
        try {
            StanLexer stanLexer = new StanLexer(CharStreams.fromString(file));
            CommonTokenStream tokens = new CommonTokenStream(stanLexer);
            StanParser stanParser = new StanParser(tokens);
            return stanParser;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Template2Parser getTemplateParserFromString(String file) {
        try {
            Template2Lexer template2Lexer = new Template2Lexer(CharStreams.fromString(file));
            CommonTokenStream tokens = new CommonTokenStream(template2Lexer);
            Template2Parser templateParser = new Template2Parser(tokens);
            return templateParser;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<TestFile> getTestFiles() {


        FileInputStream fis;
        try {
            fis = new FileInputStream(TESTFILES);
            JsonReader jsonReader = Json.createReader(fis);
            JsonArray jsonArray = jsonReader.readArray();
            ArrayList<TestFile> testFiles = new ArrayList<>();

            for (JsonObject j : jsonArray.getValuesAs(JsonObject.class)) {
                if (!(j.containsKey("skip") && j.getBoolean("skip"))) {
                    TestFile testFile = new TestFile(j);
                    testFiles.add(testFile);
                }
            }

            return testFiles;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static ArrayList<String[]> getTestFiles(boolean getnew) {
        String testfilesFile = "src/main/resources/TESTFILES.json";
        FileInputStream fis;
        try {
            fis = new FileInputStream(testfilesFile);
            JsonReader jsonReader = Json.createReader(fis);
            JsonArray jsonArray = jsonReader.readArray();
            ArrayList<String[]> testfiles = new ArrayList<>();

            for (JsonObject j : jsonArray.getValuesAs(JsonObject.class)) {
                if (getnew) {
                    if (!j.containsKey("new") || !j.getBoolean("new")) {
                        continue;
                    }
                }


                if (!(j.containsKey("skip") && j.getBoolean("skip"))) {

                    if (j.containsKey("data")) {
                        String[] testfile = new String[3];
                        testfile[0] = j.getString("file");
                        testfile[1] = j.getString("dir");
                        testfile[2] = j.getString("data");

                        testfiles.add(testfile);
                    } else {
                        String[] testfile = new String[2];
                        testfile[0] = j.getString("file");
                        testfile[1] = j.getString("dir");
                        if (!testfile[0].contains("1200")) {
                            //continue;
                        }
                        testfiles.add(testfile);
                    }

                }
            }

            return testfiles;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static int getDataSize(String templatefile) {
        File file = new File(templatefile);
        String match = Unix4j.grep("^\\S+\\s*:[^=]", file).toStringResult();
        return match.length();
    }

    public static void restoreTestFiles() {
        ArrayList<TestFile> testfiles = getTestFiles();
        for (TestFile testfileinfo : testfiles) {
//            String backupfile = testfileinfo[0].replaceAll("\\.stan", ".bak").replaceAll("\\.template", ".bak");
//            String stanbackupfile = backupfile.replaceAll("\\.bak", ".stan.original");
            String backupfile = testfileinfo.getBackupfile();
            String ppsbackupfile = testfileinfo.getPpsbackupfile();
            String ppsfile = testfileinfo.getPpsfile();

            try {
                String originalContent = new String(Files.readAllBytes(Paths.get(backupfile)));
                FileWriter fileWriter = new FileWriter(testfileinfo.getTestfile());
                fileWriter.write(originalContent);
                fileWriter.close();
                System.out.println("Restored " + testfileinfo.getTestfile());

                String stanoriginalContent = new String(Files.readAllBytes(Paths.get(ppsbackupfile)));
                fileWriter = new FileWriter(ppsfile);
                fileWriter.write(stanoriginalContent);
                fileWriter.close();
                System.out.println("Restored stan file " + ppsfile);

                //data restore
                if (testfileinfo.getDatafile() != null) {
                    String backupdata = testfileinfo.getDatabackupfile();
                    originalContent = new String(Files.readAllBytes(Paths.get(backupdata)));
                    fileWriter = new FileWriter(testfileinfo.getDatafile());
                    fileWriter.write(originalContent);
                    fileWriter.close();
                    System.out.println("Restored data " + testfileinfo.getDatafile());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    public static String getStringFromContext(ParserRuleContext ctx) {
        return ctx.start.getInputStream().getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
    }

    public static int getSLOC(String code) {
        String[] lines = code.split("\n");
        int sloc = 0;
        for (String line : lines) {
            if (line.trim().isEmpty() || line.trim().startsWith("//"))
                continue;
            sloc++;
        }

        return sloc;
    }

    public static Integer[] computePartsSum(String testfile) {
        Map<Integer, Integer> map = reductionMap.get(testfile);
        int sum_basic = 0;
        int sum_domain = 0;
        try {
            for (Integer key : map.keySet()) {
                if (isDomain(key)) {
                    sum_domain += map.get(key);
                } else {
                    sum_basic += map.get(key);
                }
            }
        } catch (Exception e) {
            System.out.println("No transformations found");
            return new Integer[]{sum_basic, sum_domain};
        }

        return new Integer[]{sum_basic, sum_domain};
    }

    public static int getPartsCount(String filecontent) {
        if (TestMinimizer.pps.contains("stan")) {
            return PartsCounter.CountFromString(filecontent);
        } else if (TestMinimizer.pps.contains("pyro")) {
            return PythonPartsCounter.CountFromString(filecontent);
        }

        return 0;
    }

    public static void createReportTable(String dir) {
        ArrayList<TestFile> testfiles = getTestFiles();
        FileWriter reportWriter = null;
        double srr_sum = 0.0;
        try {
            reportWriter = new FileWriter(dir + "/report.csv");
            reportWriter.write("TC, SLOC, RSLOC, SP, RP, Distance, Data, Samples, Basic, Domain, Time, CompileTime, RunTime, SRR, BP, DP\n");

            for (TestFile testfile : testfiles) {
                String originalFile = testfile.getBackupfile();
                String newFile = testfile.getTestfile();
                String filename = testfile.getFilename();
                String originalContent = new String(Files.readAllBytes(Paths.get(originalFile)));
                String newContent = new String(Files.readAllBytes(Paths.get(newFile)));

                String stanfilename = testfile.getPpsfile();
                String originalstanfilename = testfile.getPpsbackupfile();
                String stanoriginalContent = new String(Files.readAllBytes(Paths.get(originalstanfilename)));
                String stannewContent = new String(Files.readAllBytes(Paths.get(stanfilename)));

                reportWriter.write(filename + ", "); //filename
                reportWriter.write(getSLOC(stanoriginalContent) + ", ");
                reportWriter.write(getSLOC(stannewContent) + ", ");
//                reportWriter.write(getSLOC(originalContent)+", ");
//                reportWriter.write(getSLOC(newContent)+", ");
                int oldCount = 0, newCount = 0;
                oldCount = getPartsCount(stanoriginalContent);
                newCount = getPartsCount(stannewContent);
//                oldCount = PartsCounter.Count(originalstanfilename);
//                newCount = PartsCounter.Count(stanfilename);
//                if(testfile[0].endsWith("template")){
//                    oldCount = TemplatePartsCounter.Count(originalFile);
//                    newCount = TemplatePartsCounter.Count(newFile);
//                }
//                else {
//                    oldCount = PartsCounter.Count(originalFile);
//                    newCount = PartsCounter.Count(newFile);
//                }

                reportWriter.write(oldCount + ", ");
                reportWriter.write(newCount + ", ");

                int distance = new LevenshteinDistance().apply(originalContent, newContent);

                reportWriter.write(distance + ", ");
                if (testfile.getDatafile() != null) {
                    String reducedData = new String(Files.readAllBytes(Paths.get(testfile.getDatafile())));
                    String originalData = new String(Files.readAllBytes(Paths.get(testfile.getDatabackupfile())));
                    reducedData = reducedData.replaceAll("\\s", "");
                    originalData = originalData.replaceAll("\\s", "");
                    if (originalData.length() == 0) {
                        reportWriter.write("0, ");
                    } else {
                        double savings = (originalData.length() - reducedData.length() + 0.0) / originalData.length();
                        reportWriter.write(savings + ", ");
                    }
                } else if (!TestMinimizer.pps.contains("stan")) {
                    int reducedData = getDataSize(testfile.getTestfile());
                    int originalData = getDataSize(testfile.getBackupfile());
                    if (originalData == 0) {
                        reportWriter.write("0, ");
                    } else {
                        double savings = (originalData - reducedData + 0.0) / originalData;
                        reportWriter.write(savings + ", ");
                    }
                } else {
                    reportWriter.write("NA,");
                }

                if (tool.testmin.templatetransformers.SamplerReducer.samplingMap.containsKey(newFile)) {
                    String type = tool.testmin.templatetransformers.SamplerReducer.inferenceType.get(newFile);
                    if (type.equals("variational")) {
                        // iterations for variational
                        reportWriter.write(tool.testmin.templatetransformers.SamplerReducer.samplingMap.get(newFile) + "/");
                        reportWriter.write(tool.testmin.templatetransformers.SamplerReducer.original_samplingMap.get(newFile) + "i, ");
                    } else {
                        reportWriter.write(tool.testmin.templatetransformers.SamplerReducer.samplingMap.get(newFile) + "/");
                        reportWriter.write(tool.testmin.templatetransformers.SamplerReducer.original_samplingMap.get(newFile) + ", ");
                    }
                } else {
                    reportWriter.write("NA, ");
                }

                String transformationStuff = printTransformations(newFile);

                String[] lines = transformationStuff.trim().split("\n");

                String domain = lines[lines.length - 2].split(":")[1].trim();
                String basic = lines[lines.length - 1].split(":")[1].trim();
                reportWriter.write(basic + ", ");
                reportWriter.write(domain + ", ");

                long totaltime = 0;
                long compiletime = 0;

                try {
                    totaltime = TimingMap.get(newFile);
                    reportWriter.write(totaltime + "ms,");
                } catch (Exception e) {
                    reportWriter.write("0ms,");
                }

                try {
                    compiletime = CompileTime.get(newFile);
                    reportWriter.write(compiletime + "ms,");
                } catch (Exception e) {
                    reportWriter.write("0ms,");
                }

                try {
                    long runtime = totaltime - compiletime;
                    reportWriter.write(runtime + "ms,");
                } catch (Exception e) {
                    reportWriter.write("0ms,");
                }


                double srr = (0.0 + oldCount - newCount) / oldCount;
                reportWriter.write(String.format("%.2f", srr) + ",");
                srr_sum += srr;


                try {
                    Integer[] counts = computePartsSum(newFile);
                    reportWriter.write(counts[0] + ",");
                    reportWriter.write(Integer.toString(counts[1]));
                } catch (Exception e) {

                }

                reportWriter.write("\n");
            }

            reportWriter.flush();
            reportWriter.close();
        } catch (Exception E) {
            E.printStackTrace();
        }


    }

    public static void createReport(long time, String dir_suffix) {
        ArrayList<TestFile> testfiles = getTestFiles();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        try {
            // create the directory

            String newdir = "out/diag_" + dir_suffix;
            new File(newdir).mkdirs();

            FileWriter reportWriter = new FileWriter(newdir + "/report.txt");
            FileWriter sampleWriter = new FileWriter(newdir + "/samples");
            int totalSuccess = 0;
            int totalFailures = 0;
            for (TestFile testfile : testfiles) {
                String originalFile = testfile.getBackupfile();
                String newFile = testfile.getTestfile();
                String filename = testfile.getFilename();

                String originalContent = new String(Files.readAllBytes(Paths.get(originalFile)));
                String newContent = new String(Files.readAllBytes(Paths.get(newFile)));


                //stan code
                String stanfile_original = testfile.getPpsbackupfile();
                String stanfile_new = testfile.getPpsfile();
                String stan_newcontent = new String(Files.readAllBytes(Paths.get(stanfile_new)));
                //report code
                reportWriter.write("============================= " + filename + " =============================\n");
                reportWriter.write("Before: " + getSLOC(originalContent) + "\n");
                reportWriter.write("After : " + getSLOC(newContent) + "\n");
                reportWriter.write("Diff: \n");
                //reportWriter.write(getDiff(originalFile, newFile) + "\n");
                LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
                int distance = levenshteinDistance.apply(originalContent, newContent);
                reportWriter.write("Distance : " + distance);

                // report samples
                if (tool.testmin.templatetransformers.SamplerReducer.samplingMap.containsKey(newFile)) {
                    reportWriter.write("Samples : " + tool.testmin.templatetransformers.SamplerReducer.samplingMap.get(newFile) + "\n");
                    reportWriter.write("Samples Before : " + tool.testmin.templatetransformers.SamplerReducer.original_samplingMap.get(newFile) + "\n");

                    //write samples
                    sampleWriter.write(filename + ":");
                    sampleWriter.write(tool.testmin.templatetransformers.SamplerReducer.samplingMap.get(newFile) + "\n");
                } else {
                    reportWriter.write("Samples : NA\n");
                }

                //report data
                if (testfile.getDatafile() != null) {
                    String reducedData = new String(Files.readAllBytes(Paths.get(testfile.getDatafile())));
                    String originalData = new String(Files.readAllBytes(Paths.get(testfile.getDatabackupfile())));
                    reducedData = reducedData.replaceAll("\\s", "");
                    originalData = originalData.replaceAll("\\s", "");
                    reportWriter.write("Reduced : " + reducedData.length() + "\n");
                    reportWriter.write("Original : " + originalData.length() + "\n");
                    double savings = (originalData.length() - reducedData.length() + 0.0) / originalData.length();
                    reportWriter.write("Savings : " + savings + "\n");
                }

                // log the transformations success and failures
                reportWriter.write(printTransformations(newFile));

                // log total success and failure count
                Integer[] counts = getTransformationPerformanceCount(newFile);
                reportWriter.write("success: " + counts[0] + ", failures: " + counts[1] + "\n");
                totalSuccess += counts[0];
                totalFailures += counts[1];

                // log sequence of transformations applied
                if (TemplateTransformationMap.containsKey(newFile)) {
                    reportWriter.write(TransformationSequence.get(newFile));
                }
                reportWriter.flush();

                // write the new file
                FileWriter reducedFile = new FileWriter(newdir + "/r_" + filename);
                reducedFile.write(newContent);
                reducedFile.close();

                // write new stan file
                reducedFile = new FileWriter(newdir + "/r_" + testfile.getPpsfilename());
                reducedFile.write(stan_newcontent);
                reducedFile.close();

                if (testfile.getDatafile() != null) {
                    // save data
                    String newData = new String(Files.readAllBytes(Paths.get(testfile.getDatafile())));
                    FileWriter reducedData = new FileWriter(newdir + "/r_" + testfile.getDatafile().split("/")[testfile.getDatafile().split("/").length - 1]);
                    reducedData.write(newData);
                    reducedData.close();
                }

            }

            reportWriter.write("Total success : " + totalSuccess + ", failures: " + totalFailures + "\n");
            reportWriter.write("Success rate: " + (totalSuccess + 0.0) / (totalFailures + totalSuccess + 0.0) + "\n");
            reportWriter.write("Total time : " + time / 1000 + " seconds \n");
            reportWriter.write(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()) + "\n");
            System.out.println("Total time : " + time / 1000 + " seconds \n");
            reportWriter.write("Order: " + order + "\n");
            // print all averages and median

            reportWriter.close();

            sampleWriter.close();

            createReportTable(newdir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateTransformations(String testfile, int transformation, boolean success) {
        if (!Transformations.containsKey(testfile)) {
            Transformations.put(testfile, new HashMap<>());
        }

        if (!TransformationSequence.containsKey(testfile)) {
            TransformationSequence.put(testfile, "");
        }

        String sequence = TransformationSequence.get(testfile);

        Map<Integer, Integer[]> transformationMap = Transformations.get(testfile);
        if (!transformationMap.containsKey(transformation)) {
            transformationMap.put(transformation, new Integer[]{0, 0});
        }
        Integer[] success_fail_count = transformationMap.get(transformation);
        if (success) {
            // update success count
            success_fail_count[0] += 1;
            sequence += transformation + "(S)-";

        } else {
            // update failure count
            success_fail_count[1] += 1;
            sequence += transformation + "(F)-";
        }

        // update sequence
        TransformationSequence.put(testfile, sequence);
        transformationMap.put(transformation, success_fail_count);
        //System.out.println("[" + testfile.split("/")[1] + "] transformation updated");

    }

    public static String getTransformationString(int t) {
        //System.out.println("Trans : " + t);
        return TemplateTransformationMap.get(t).getCanonicalName();
    }

    public static Integer[] getTransformationPerformanceCount(String testfile) {
        Map<Integer, Integer[]> transformations = Transformations.get(testfile);

        int success = 0;
        int failures = 0;
        if (transformations != null) {
            for (int t : transformations.keySet()) {
                Integer[] counts = transformations.get(t);
                success += counts[0];
                failures += counts[1];
            }
        }

        Integer[] totalCounts = new Integer[]{success, failures};
        return totalCounts;
    }

    public static String printTransformations(String testfile) {
        Map<Integer, Integer[]> transformations = Transformations.get(testfile);
        String output = "";
        int domain = 0;
        int basic = 0;

        if (transformations != null) {
            for (int t : transformations.keySet()) {
                Integer[] counts = transformations.get(t);
                output += getTransformationString(t) + " : Success = " + counts[0] + ", Failures = " + counts[1] + "\n";
                if (isDomain(t))
                    domain += counts[0];
                else
                    basic += counts[0];
            }
        }

        output += "Domain: " + domain + "\n";
        output += "Basic: " + basic + "\n";
        return output;
    }

    public static boolean isDomain(int transformation) {
        return (transformation == 0) ||
                (transformation == 3) ||
                (transformation == 9) ||
                (transformation == 10) ||
                (transformation == 11) ||
                (transformation == 12) ||
                (transformation == 13);
    }

    public static void updateCompileTime(String testfile, long time) {
        if (!CompileTime.containsKey(testfile)) {
            CompileTime.put(testfile, 0L);
        }
        CompileTime.put(testfile, CompileTime.get(testfile) + time);
    }

    public static boolean runTransformedCode(String curFileContent,
                                             String newContent,
                                             int transformer,
                                             MyLogger logger,
                                             TestFile testFile) {
        return runTransformedCode(curFileContent, newContent, transformer, logger, testFile, false);
    }

    public static boolean runTransformedCode(String curFileContent,
                                             String newContent,
                                             int transformer,
                                             MyLogger logger,
                                             TestFile testFile,
                                             boolean datachanged) {
        try {
            if (newContent.equals(curFileContent) && !datachanged) {
                // dont test if there are no changes made by the transformers
                logger.print("No changes... returning", true);
                TMUtil.updateTransformations(testFile.getTestfile(), transformer, false);
                TMUtil.updateCompileTime(testFile.getTestfile(), 0L);
                return false;
            }

            String stanfilename = testFile.getPpsfile();
            String oldstancode = new String(Files.readAllBytes(Paths.get(stanfilename)));
            String testfile = testFile.getTestfile();
            String testdir = testFile.getTestdir();
            String runner = null;
            if (TMUtil.RunCov) {
                runner = "coverage/exp/p_measurecov.sh";
            } else {
                runner = testdir + "/run.sh "+testdir;
            }

            FileWriter writer = new FileWriter(testfile);
            //remove extra spaces
            writer.write(newContent.trim());
            writer.close();

            Process p = null;

            Date compileStart = new Date(System.currentTimeMillis());
            if (tool.testmin.templatetransformers.SamplerReducer.samplingMap.containsKey(testfile)) {
                int samples = tool.testmin.templatetransformers.SamplerReducer.samplingMap.get(testfile);
                System.out.println("Running with samples: " + samples);
                p = Runtime.getRuntime().exec(runner + " " + testdir + " " + samples);
            } else {
                System.out.println("Running without samples: ");
                p = Runtime.getRuntime().exec(runner + " " + testdir);
            }

            p.waitFor();
            Date compileEnd = new Date(System.currentTimeMillis());

            //update compile time... may be needed
            long compileTime = compileEnd.getTime() - compileStart.getTime();
            TMUtil.updateCompileTime(testfile, compileTime);
            logger.print("Compile Time : " + compileTime, true);

            InputStreamReader reader = new InputStreamReader(p.getInputStream());
            String output = com.google.common.io.CharStreams.toString(reader);
            String error = com.google.common.io.CharStreams.toString(new InputStreamReader(p.getErrorStream()));
            logger.print(output, true);
            logger.print(error, true);

            if (output.contains("Passed")) {
                logger.print("Passed", true);
//                this.curFileContent = newContent.trim();
//                if(this.datafile != null) {
//                    this.curDataContent = new String(Files.readAllBytes(Paths.get(this.datafile)));
//                }
                TMUtil.updateTransformations(testfile, transformer, true);
//                Util.updateReductionCount(testfile, transformer, curFileContent, newContent);
                String newstancode = new String(Files.readAllBytes(Paths.get(stanfilename)));
                TMUtil.updateReductionCount(testfile, transformer, oldstancode, newstancode);
            } else {
                // Recovery phase
                logger.print("Failed", true);
                writer = new FileWriter(testfile);
                writer.write(curFileContent);
                writer.flush();
                writer.close();

                //write stan file
                logger.print("Writeback PPS file", true);
                writer = new FileWriter(stanfilename);
                writer.write(oldstancode);
                writer.flush();
                writer.close();
//                if(this.datafile != null) {
//                    writer = new FileWriter(datafile);
//                    writer.write(curDataContent);
//                    writer.flush();
//                    writer.close();
//                }

//                if(transformer == 12){
//                    SamplerReducer.recover(logger);
//                }

                TMUtil.updateTransformations(testfile, transformer, false);
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static boolean runTransformedCode(String curFileContent,
                                             String newContent,
                                             int transformer,
                                             MyLogger logger,
                                             String testdir,
                                             String testfile,
                                             boolean datachanged) {
        try {
            if (newContent.equals(curFileContent) && !datachanged) {
                // dont test if there are no changes made by the transformers
                logger.print("No changes... returning", true);
                TMUtil.updateTransformations(testfile, transformer, false);
                TMUtil.updateCompileTime(testfile, 0L);
                return false;
            }

            String stanfilename = testfile.replaceAll("\\.template", ".stan");
            String oldstancode = new String(Files.readAllBytes(Paths.get(stanfilename)));

            String runner = testdir + "/run.sh";

            FileWriter writer = new FileWriter(testfile);
            //remove extra spaces
            writer.write(newContent.trim());
            writer.close();

            Process p = null;

            Date compileStart = new Date(System.currentTimeMillis());
            if (tool.testmin.templatetransformers.SamplerReducer.samplingMap.containsKey(testfile)) {
                int samples = tool.testmin.templatetransformers.SamplerReducer.samplingMap.get(testfile);
                System.out.println("Running with samples: " + samples);
                p = Runtime.getRuntime().exec(runner + " " + testdir + " " + samples);
            } else {
                System.out.println("Running without samples: ");
                p = Runtime.getRuntime().exec(runner + " " + testdir);
            }

            p.waitFor();
            Date compileEnd = new Date(System.currentTimeMillis());

            //update compile time... may be needed
            long compileTime = compileEnd.getTime() - compileStart.getTime();
            TMUtil.updateCompileTime(testfile, compileTime);
            logger.print("Compile Time : " + compileTime, true);

            InputStreamReader reader = new InputStreamReader(p.getInputStream());
            String output = com.google.common.io.CharStreams.toString(reader);
            String error = com.google.common.io.CharStreams.toString(new InputStreamReader(p.getErrorStream()));
            logger.print(output, true);
            logger.print(error, true);

            if (output.contains("Passed")) {
                logger.print("Passed", true);
//                this.curFileContent = newContent.trim();
//                if(this.datafile != null) {
//                    this.curDataContent = new String(Files.readAllBytes(Paths.get(this.datafile)));
//                }
                TMUtil.updateTransformations(testfile, transformer, true);
//                Util.updateReductionCount(testfile, transformer, curFileContent, newContent);
                String newstancode = new String(Files.readAllBytes(Paths.get(stanfilename)));
                TMUtil.updateReductionCount(testfile, transformer, oldstancode, newstancode);
            } else {
                // Recovery phase
                logger.print("Failed", true);
                writer = new FileWriter(testfile);
                writer.write(curFileContent);
                writer.flush();
                writer.close();

                //write stan file
                logger.print("Writeback stan file", true);
                writer = new FileWriter(stanfilename);
                writer.write(oldstancode);
                writer.flush();
                writer.close();
//                if(this.datafile != null) {
//                    writer = new FileWriter(datafile);
//                    writer.write(curDataContent);
//                    writer.flush();
//                    writer.close();
//                }

//                if(transformer == 12){
//                    SamplerReducer.recover(logger);
//                }

                TMUtil.updateTransformations(testfile, transformer, false);
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static String getParamName(ParserRuleContext ctx) {
        Template2Parser.PriorContext pctx = (Template2Parser.PriorContext) ctx;
        if (pctx.expr() instanceof Template2Parser.RefContext)
            return ((Template2Parser.RefContext) pctx.expr()).ID().getText();
        else
            return ((Template2Parser.Array_accessContext) pctx.expr()).ID().getText();
    }

    public static boolean isInteger(String type) {
        List<String> ints = Arrays.asList("i", "i+", "0i+", "b");
        for (String x : ints) {
            if (type.equalsIgnoreCase(x)) {
                return true;
            }
        }
        return false;

    }

    public static String generate_primitive(String data_type) {
        String x_data = null;
        if (data_type.equals("i")) {
            x_data = Integer.toString(new Random().nextInt(100));
        } else if (data_type.equals("f")) {
            x_data = (new Random().nextDouble() * 200 - 100) + "";
        } else if (data_type.equals("p")) {
            x_data = new Random().nextDouble() + "";
        } else if (data_type.equals("f+")) {
            x_data = new Random().nextDouble() * 100.0 + "";
        } else if (data_type.equals("0f+")) {
            x_data = new Random().nextDouble() * 100.0 + "";
        } else if (data_type.equals("i+")) {

            x_data = new Random().nextInt(100) + "";
        } else if (data_type.equals("b")) {
            x_data = new Random().nextInt(2) + "";
        } else if (data_type.equals("(0,1)")) {
            x_data = new Random().nextDouble() + "";
        } else if (data_type.equals("0i+")) {
            x_data = new Random().nextInt(100) + "";
        } else {
            System.out.println(data_type + ": Not Implemented");
        }

        return x_data;
    }

    public static boolean includesSupport(String candidate_support, String support) {
        if (candidate_support.equals("x")) {
            return true;
        } else if (support.equals("f+")) {
            return Arrays.asList(new String[]{"f+", "(0,1)", "[alpha, beta]"}).contains(candidate_support);
        } else if (support.equals("i+")) {
            return Arrays.asList(new String[]{"i+", "(0,1)"}).contains(candidate_support);
        } else if (support.equals("f")) {
            return Arrays.asList(new String[]{"f", "f+", "(0,1)", "p", "0f+", "[alpha, beta]"}).contains(candidate_support);
        } else if (support.equals("i")) {
            return Arrays.asList(new String[]{"i+", "i", "0i+"}).contains(candidate_support);
        } else if (support.equals("p")) {
            return Arrays.asList(new String[]{"(0,1)", "p", "[alpha, beta]"}).contains(candidate_support);
        } else if (support.equals("(0,1)")) {
            return Arrays.asList(new String[]{"(0,1)", "[alpha, beta]"}).contains(candidate_support);
        } else if (support.equals("0i+")) {
            return Arrays.asList(new String[]{"0i+", "i+", "b"}).contains(candidate_support);
        } else if (support.equals("0f+")) {
            return Arrays.asList(new String[]{"f+", "(0,1)", "p", "0f+", "[alpha, beta]"}).contains(candidate_support);
        } else if (support.equals("simplex")) {
            return support.equals(candidate_support);
        } else if (support.equals("[f]")) {
            return support.equals(candidate_support);
        } else if (support.equals("b")) {
            return support.equals(candidate_support);
        } else if (support.equals("[[f]]")) {
            return false;
        } else if (support.equals("[0,N]")) {
            return Arrays.asList(new String[]{"[0,N]", "b", "0i+", "[alpha, beta]"}).contains(candidate_support);
        } else {
            System.out.println("Unsupported type : " + support);
            return false;
        }
    }
}

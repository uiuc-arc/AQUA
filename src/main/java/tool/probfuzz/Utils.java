package tool.probfuzz;
import grammar.cfg.CFGBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.rng.DefaultRandom;
import org.nd4j.linalg.api.rng.distribution.Distribution;
import org.nd4j.linalg.factory.Nd4j;

import translators.Edward2Translator;
import translators.PsiTranslator;
import translators.PyroTranslator;
import translators.StanTranslator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Utils {

    public static boolean STAN_ENABLED;
    public static boolean PSI_ENABLED;
    public static boolean EDWARD2_ENABLED;
    public static boolean PYRO_ENABLED;
    public static String STANRUNNER;
    public static String PSIRUNNER;
    public static String EDWARD2RUNNER;
    public static String PYRORUNNER;


    static {
        Properties properties = new Properties();
        try {
            String CONFIGURATIONFILE = "src/main/resources/config.properties";
            FileInputStream fileInputStream = new FileInputStream(CONFIGURATIONFILE);
            properties.load(fileInputStream);
            STAN_ENABLED =  Boolean.parseBoolean(properties.getProperty("stan.enabled"));
            PSI_ENABLED = Boolean.parseBoolean(properties.getProperty("psi.enabled"));
            EDWARD2_ENABLED = Boolean.parseBoolean(properties.getProperty("edward2.enabled"));
            PYRO_ENABLED = Boolean.parseBoolean(properties.getProperty("pyro.enabled"));

            STANRUNNER = properties.getProperty("stan.script");
            PSIRUNNER = properties.getProperty("psi.script");
            EDWARD2RUNNER = properties.getProperty("edward2.script");
            PYRORUNNER = properties.getProperty("pyro.script");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static List<String> getEnabledPPS(){
        return new ArrayList<String>(Arrays.asList("stan", "pyro", "edward2", "psi"));
    }

    public static INDArray generateData(String datatype, int[] size){
        if(datatype.equalsIgnoreCase("f") || datatype.equalsIgnoreCase("float")){
            return new DefaultRandom().nextFloat(size);
        }
        else if(datatype.equalsIgnoreCase("f+")){
            UniformRealDistribution u = new UniformRealDistribution(0, Float.MAX_VALUE);
            Distribution distribution = Nd4j.getDistributions().createUniform(0, Float.MAX_VALUE);
            return distribution.sample(size);
        }
        else if(datatype.equalsIgnoreCase("int") || datatype.equalsIgnoreCase("i")){
            return new DefaultRandom().nextInt(size);
        }
        else{
            System.out.println("Not handled: "+datatype);
        }

        return null;
    }

    public static void translateToStan(String templateFile, String outputdir, String filePrefix){
        File file = new File(templateFile);
        StanTranslator stanTranslator = new StanTranslator();
        try {
            stanTranslator.translate((new CFGBuilder(file.getAbsolutePath(), null, false)).getSections());
            String stanCode = stanTranslator.getCode();
            String stanData = stanTranslator.getData();
            File f = new File(outputdir);
            if(!f.exists()){
                f.mkdirs();
            }
            Files.write(Paths.get(outputdir+"/"+filePrefix+".stan"), stanCode.getBytes());
            Files.write(Paths.get(outputdir+"/"+filePrefix+".data.R"), stanData.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void translateToPyro(String templateFile, String outputdir, String filePrefix){
        File file = new File(templateFile);
        PyroTranslator pyroTranslator = new PyroTranslator();
        try {
            pyroTranslator.translate((new CFGBuilder(file.getAbsolutePath(), null, false)).getSections());
            String pyroCode = pyroTranslator.getCode();
            File f = new File(outputdir);
            if(!f.exists()){
                f.mkdirs();
            }
            Files.write(Paths.get(outputdir+"/"+filePrefix+".py"), pyroCode.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void translateToEdward2(String templateFile, String outputdir, String filePrefix){
        File file = new File(templateFile);
        Edward2Translator edward2Translator = new Edward2Translator();
        try {
            edward2Translator.translate((new CFGBuilder(file.getAbsolutePath(), null, false)).getSections());
            String pyroCode = edward2Translator.getCode();
            File f = new File(outputdir);
            if(!f.exists()){
                f.mkdirs();
            }
            Files.write(Paths.get(outputdir+"/"+filePrefix+".py"), pyroCode.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void translateToPSI(String templateFile, String outputdir, String filePrefix){
        File file = new File(templateFile);
        PsiTranslator psiTranslator = new PsiTranslator();
        try {
            psiTranslator.translate((new CFGBuilder(file.getAbsolutePath(), null, false)).getSections());
            String pyroCode = psiTranslator.getCode();
            File f = new File(outputdir);
            if(!f.exists()){
                f.mkdirs();
            }
            Files.write(Paths.get(outputdir+"/"+filePrefix+".psi"), pyroCode.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Pair<String, String> runPsi(String filename){
        Process p = null;
        try {
            p = Runtime.getRuntime().exec( PSIRUNNER + " " + filename + " " );

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

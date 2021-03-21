package tool.probfuzz;



import org.apache.commons.lang3.tuple.Pair;
import utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;

public class Main {
    public static void main(String[] args){
        // template to fuzz
        String templateFile = args[0];
        // number of programs to generate
        int programs = Integer.parseInt(args[1]);
        // generate programs
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String output_dir = "output/fuzz_" + timestamp.getTime()/1000;
        new File(output_dir).mkdirs();
        System.out.println("Output Directory: " + output_dir);
        PLogger logger = new PLogger(output_dir);
        for(int i = 1; i <= programs; i++){
            logger.print("Program: " + i, true);
            TemplatePopulator populator = new TemplatePopulator(templateFile, logger);
            populator.transformCode();
            String transformedCode = populator.getTransformedCode();
            String filePrefix = "prog_"+i;
            String templatePath = output_dir + "/" + filePrefix;
            String newTemplateFile = templatePath + "/" + filePrefix + ".template";
            boolean created =new File(templatePath).mkdirs();
            try {
                Files.write(Paths.get(newTemplateFile), transformedCode.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(Utils.STAN_ENABLED) {
                logger.print("Generating and running Stan program", true);
                Utils.translateToStan(newTemplateFile, templatePath + "/stan", filePrefix);
                Pair<String, String> results = CommonUtils.runCode(Paths.get(templatePath+"/stan/"+filePrefix+".stan").toAbsolutePath().toString(),
                        Paths.get(templatePath+"/stan/"+filePrefix+".data.R").toAbsolutePath().toString(), Utils.STANRUNNER);
                File resultsFile = new File(templatePath +"/stan/output.txt");
                try {
                    Files.write(Paths.get(resultsFile.getAbsolutePath()), results.getLeft().getBytes());
                    Files.write(Paths.get(resultsFile.getAbsolutePath()), results.getRight().getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(Utils.PYRO_ENABLED){
                logger.print("Generating and running Pyro program", true);
                Utils.translateToPyro(newTemplateFile, templatePath + "/pyro", filePrefix);
                Pair<String, String> results = CommonUtils.runCode(Paths.get(templatePath+"/pyro/"+filePrefix+".py").toAbsolutePath().toString(),
                        Utils.PYRORUNNER);
                File resultsFile = new File(templatePath +"/pyro/output.txt");
                try {
                    Files.write(Paths.get(resultsFile.getAbsolutePath()), results.getLeft().getBytes());
                    Files.write(Paths.get(resultsFile.getAbsolutePath()), results.getRight().getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(Utils.EDWARD2_ENABLED){
                logger.print("Generating and running Edward2 program", true);
                Utils.translateToEdward2(newTemplateFile, templatePath + "/edward2", filePrefix);
                Pair<String, String> results = CommonUtils.runCode(Paths.get(templatePath+"/edward2/"+filePrefix+".py").toAbsolutePath().toString(),
                        Utils.EDWARD2RUNNER);
                File resultsFile = new File(templatePath +"/edward2/output.txt");
                try {
                    Files.write(Paths.get(resultsFile.getAbsolutePath()), results.getLeft().getBytes());
                    Files.write(Paths.get(resultsFile.getAbsolutePath()), results.getRight().getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
//            if(Utils.PSI_ENABLED){
//                Utils.translateToPSI(newTemplateFile, templatePath + "/psi", filePrefix);
//                Pair<String, String> results = CommonUtils.runCode(Paths.get(templatePath+"/psi/"+filePrefix+".psi").toAbsolutePath().toString(),
//                        Utils.PSIRUNNER);
//                File resultsFile = new File(templatePath +"/psi/output.txt");
//                try {
//                    Files.write(Paths.get(resultsFile.getAbsolutePath()), results.getLeft().getBytes());
//                    Files.write(Paths.get(resultsFile.getAbsolutePath()), results.getRight().getBytes(), StandardOpenOption.APPEND);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        }
    }
}

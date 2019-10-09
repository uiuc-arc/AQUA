package tests;

import grammar.DataParser;
import grammar.StanParser;
import grammar.cfg.CFGBuilder;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import translators.Stan2IRTranslator;
import utils.DataReader;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileReader;

public class TestStan2IRTranslation {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void test1(){

        try {
            StanParser parser = Utils.readStanFile("src/test/resources/stan/stochastic-volatility.stan");
            ParseTreeWalker walker = new ParseTreeWalker();
            Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator("src/test/resources/stan/stochastic-volatility.stan", "src/test/resources/stan/stochastic-volatility.data.R");
            walker.walk(stan2IRTranslator, parser.program());
            String code = stan2IRTranslator.getCode();
            File file = temporaryFolder.newFile();
            FileUtils.writeStringToFile(file, code);
            System.out.println(code);
            CFGBuilder builder = new CFGBuilder(file.getAbsolutePath(), "src/test/resources/stochastic-volatility.png");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cant get path");
        }
    }

    @Test
    public void test2(){
        try {
            Stan2IRTranslator stan2IRTranslator =
                    new Stan2IRTranslator("/home/zixin/Documents/are/PPVM/templates/basic_robust3/basic_robust3.copy2",
                    "/home/zixin/Documents/are/PPVM/templates/basic_robust3/noisy.data.R");
            String code = stan2IRTranslator.getCode();
            File file = temporaryFolder.newFile();
            FileUtils.writeStringToFile(file, code);
            System.out.println(code);
            CFGBuilder builder = new CFGBuilder(file.getAbsolutePath(), "src/test/resources/basic_robust3_copy2.png");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cant get path");
        }
    }

    @Test
    public void test3(){
        DataParser parser = Utils.readDataFile("src/test/resources/stan/electric_1c_chr.data.R");
        ParseTreeWalker walker = new ParseTreeWalker();
        DataReader dataReader = new DataReader();
        walker.walk(dataReader, parser.datafile());
        dataReader.printData();
    }

    @Test
    public void test4(){

        DataParser parser = Utils.readDataFile("/home/saikat/projects/c4pp/programs/templates/stan_con/activation-based_h/activation-based_h.data.R");
        ParseTreeWalker walker = new ParseTreeWalker();
        DataReader dataReader = new DataReader();
        walker.walk(dataReader, parser.datafile());
        dataReader.printData();
    }

    @Test
    public void test6() throws IOException {
        String content = null;
        File file = new File("/home/zixin/Documents/are/c4pp/c4pp/programs/stan_con/stan_con_clean_b2/hBayesDM.run"); // For example, foo.txt
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            char[] chars = new char[(int) file.length()];
            reader.read(chars);
            content = new String(chars);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(reader != null){
                reader.close();
            }
        }
        String[] pps = content.split("\n");
        for (String pp : pps){
            System.out.println(pp);
            try {
                String pp_path = "/home/zixin/Documents/are/c4pp/c4pp/programs/stan_con/stan_con_clean_b2/" + pp;
                // StanParser parser = Utils.readStanFile(pp_path + "/" + pp + ".stan");
                // ParseTreeWalker walker = new ParseTreeWalker();
                // System.out.println(pp_path);
                // Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator(pp_path + "/" + pp + ".stan", pp_path + "/" + pp + ".data.R");
                // walker.walk(stan2IRTranslator, parser.program());
                // String code = stan2IRTranslator.getCode();
                // File tempfile = temporaryFolder.newFile();
                // FileUtils.writeStringToFile(tempfile, code);
                // System.out.println(code);
                DataParser parser = Utils.readDataFile(pp_path + "/" + pp + ".data.R");
                ParseTreeWalker walker = new ParseTreeWalker();
                DataReader dataReader = new DataReader();
                walker.walk(dataReader, parser.datafile());
                String str = dataReader.printDataString();
                System.out.println(str);
                File newfile = new File(pp_path + "/" + pp + ".template");
                FileUtils.writeStringToFile(newfile, str);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // break;
        }

    public void test5(){
        try {
            Stan2IRTranslator stan2IRTranslator =
                    new Stan2IRTranslator("src/test/resources/stan/constant-synthesis.stan",
                            "src/test/resources/stan/constant-synthesis.data.R");

            String code = stan2IRTranslator.getCode();
            File file = temporaryFolder.newFile();
            FileUtils.writeStringToFile(file, code);
            System.out.println(code);
            CFGBuilder builder = new CFGBuilder(file.getAbsolutePath(), "src/test/resources/constant-synthesis.png");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cant get path");
        }
    }
}

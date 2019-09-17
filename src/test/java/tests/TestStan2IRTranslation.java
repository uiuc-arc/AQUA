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

public class TestStan2IRTranslation {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void test1(){

        try {
            StanParser parser = Utils.readStanFile("src/test/resources/stan/stan1610.stan");
            ParseTreeWalker walker = new ParseTreeWalker();
            Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator("src/test/resources/stan/stan1610.stan", "src/test/resources/stan/stan1610.data");
            walker.walk(stan2IRTranslator, parser.program());
            String code = stan2IRTranslator.getCode();
            File file = temporaryFolder.newFile();
            FileUtils.writeStringToFile(file, code);
            System.out.println(code);
            CFGBuilder builder = new CFGBuilder(file.getAbsolutePath(), "src/test/resources/stan1610.png");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cant get path");
        }
    }

    @Test
    public void test2(){
        try {
            Stan2IRTranslator stan2IRTranslator =
                    new Stan2IRTranslator("src/test/resources/stan/electric_1c_chr.stan",
                    "src/test/resources/stan/electric_1c_chr.data.R");

            String code = stan2IRTranslator.getCode();
            File file = temporaryFolder.newFile();
            FileUtils.writeStringToFile(file, code);
            System.out.println(code);
            CFGBuilder builder = new CFGBuilder(file.getAbsolutePath(), "src/test/resources/electric_1c_chr.png");
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

        DataParser parser = Utils.readDataFile("src/test/resources/stan/gp-fit-ARD.data.R");
        ParseTreeWalker walker = new ParseTreeWalker();
        DataReader dataReader = new DataReader();
        walker.walk(dataReader, parser.datafile());
        dataReader.printData();
    }
}
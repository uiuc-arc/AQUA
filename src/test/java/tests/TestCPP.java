package tests;


import grammar.cfg.CFGBuilder;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import translators.CPPTranslator;
import translators.Stan2IRTranslator;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;

import java.io.*;

public class TestCPP {
    @Test
    public void testToTemplate(){
        try {
            String stanfile = "/Users/zixin/Documents/uiuc/summer20/small/decipher/denoise_template/denoise.stan";
            String standata ="/Users/zixin/Documents/uiuc/summer20/small/decipher/denoise_template/dog4_noisy_channel0.data.R";
            Stan2IRTranslator stan2IRTranslator =
                    new Stan2IRTranslator(stanfile,standata);
            String code = stan2IRTranslator.getCode();
            String tempFileName = stanfile.replace(".stan", "");
            String newFilePath = tempFileName + ".template";
            System.out.println(code);
            PrintWriter out = new PrintWriter(newFilePath);
            out.println(code);
            out.close();
            // CFGBuilder builder = new CFGBuilder(file.getAbsolutePath(), "src/test/resources/denoise.png");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cant get path");
        }
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Test
    public void testCPP1() throws IOException {
        String filePath = "/Users/zixin/Documents/uiuc/summer20/small/decipher/denoise_template/denoise.template";
        CFGBuilder cfgBuilder = new CFGBuilder(filePath, null);
        System.out.println(cfgBuilder.parser.template().toString());
        String outputFile = filePath.substring(0, filePath.length() - 9) + ".cctemp";
        try {
            FileOutputStream out = new FileOutputStream(outputFile);
            cfgBuilder.parser.reset();
            CPPTranslator cppTranslator = new CPPTranslator(cfgBuilder, out);
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(cppTranslator, cfgBuilder.parser.template());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

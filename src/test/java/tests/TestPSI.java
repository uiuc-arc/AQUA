package tests;

import grammar.cfg.CFGBuilder;
import org.junit.Test;
import translators.PsiTranslator;

import java.io.File;

public class TestPSI {
    @Test
    public void testBasicDistribution(){
        String dirPath= "src/test/resources/psi/probmods/";
        File folder = new File(dirPath);
        String[] files = folder.list();
        for (String file : files) {
            if(file.contains(".template")) {
                System.out.println("Parsing: " + file );
                String filename = dirPath + file;
                String outputFile = filename.substring(0, filename.length()-9) + ".psi";
                PsiTranslator trans = new PsiTranslator();
                CFGBuilder cfgBuilder = new CFGBuilder(filename, outputFile, false);
                try {
                    trans.translate(cfgBuilder.getSections());
                } catch (Exception e){

                }
            }
        }
    }
}

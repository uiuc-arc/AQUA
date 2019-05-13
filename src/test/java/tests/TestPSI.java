package tests;

import grammar.cfg.CFGBuilder;
import org.junit.Test;
import translators.PsiTranslator;

import java.io.File;
import java.io.FileOutputStream;

public class TestPSI {
    @Test
    public void testBasic(){
        String filePath= "src/test/resources/psi/basic.template";
        String outputFile = filePath.substring(0, filePath.length()-9) + ".psi";

        CFGBuilder cfgBuilder = new CFGBuilder(filePath, outputFile, false);
        PsiTranslator trans = new PsiTranslator();
        try{
            FileOutputStream out = new FileOutputStream(outputFile);
            trans.setOut(out);
        } catch(Exception e){
            e.printStackTrace();
            return;
        }
        try {
            trans.translate(cfgBuilder.getSections());
            trans.run(outputFile);
        } catch (Exception e){
            e.printStackTrace();
        }

    }
    @Test
    public void testBasicDistribution(){
        String dirPath= "src/test/resources/psi/basic_distributions/";
        File folder = new File(dirPath);
        String[] files = folder.list();
        for (String file : files) {
            if(file.contains(".template")) {
                System.out.println("Parsing: " + file );
                String filename = dirPath + file;
                String outputFile = filename.substring(0, filename.length()-9) + ".psi";

                PsiTranslator trans = new PsiTranslator();
                try{
                    FileOutputStream out = new FileOutputStream(outputFile);
                    trans.setOut(out);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                CFGBuilder cfgBuilder = new CFGBuilder(filename, outputFile, false);
                try {
                    trans.translate(cfgBuilder.getSections());
                    trans.run(outputFile);

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
    @Test
    public void testProbMode02(){
        String dirPath= "src/test/resources/psi/probmods/ch02_generative_models/";
        File folder = new File(dirPath);
        String[] files = folder.list();
        for (String file : files) {
            if(file.contains(".template")) {
                System.out.println("Parsing: " + file );
                String filename = dirPath + file;
                String outputFile = filename.substring(0, filename.length()-9) + ".psi";

                PsiTranslator trans = new PsiTranslator();
                try{
                    FileOutputStream out = new FileOutputStream(outputFile);
                    trans.setOut(out);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                CFGBuilder cfgBuilder = new CFGBuilder(filename, outputFile, false);
                try {
                    trans.translate(cfgBuilder.getSections());
                    trans.run(outputFile);

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
    @Test
    public void testYXY(){
        String filePath= "src/test/resources/psi/other_psi_features/y_x_y.template";
        String outputFile = filePath.substring(0, filePath.length()-9) + ".psi";

        CFGBuilder cfgBuilder = new CFGBuilder(filePath, outputFile, false);
        PsiTranslator trans = new PsiTranslator();
        try{
            FileOutputStream out = new FileOutputStream(outputFile);
            trans.setOut(out);
        } catch(Exception e){
            e.printStackTrace();
            return;
        }
        try {
            trans.translate(cfgBuilder.getSections());
            trans.run(outputFile);
        } catch (Exception e){
            e.printStackTrace();
        }

    }
    @Test
    public void testObserve(){
        String filePath= "src/test/resources/psi/basic_distributions/observe.template";
        String outputFile = filePath.substring(0, filePath.length()-9) + ".psi";

        CFGBuilder cfgBuilder = new CFGBuilder(filePath, outputFile, false);
        PsiTranslator trans = new PsiTranslator();
        try{
            FileOutputStream out = new FileOutputStream(outputFile);
            trans.setOut(out);
        } catch(Exception e){
            e.printStackTrace();
            return;
        }
        try {
            trans.translate(cfgBuilder.getSections());
            trans.run(outputFile);
        } catch (Exception e){
            e.printStackTrace();
        }

    }
    @Test
    public void testOccamsRazors() {
        String filePath = "src/test/resources/psi/probmods/bayes_occams_razor.template";
        String outputFile = filePath.substring(0, filePath.length() - 9) + ".psi";

        CFGBuilder cfgBuilder = new CFGBuilder(filePath, outputFile, false);
        PsiTranslator trans = new PsiTranslator();
        try {
            FileOutputStream out = new FileOutputStream(outputFile);
            trans.setOut(out);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        try {
            trans.translate(cfgBuilder.getSections());
            trans.run(outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testFor() {
        String filePath = "src/test/resources/psi/for.template";
        String outputFile = filePath.substring(0, filePath.length() - 9) + ".psi";

        CFGBuilder cfgBuilder = new CFGBuilder(filePath, outputFile, false);
        PsiTranslator trans = new PsiTranslator();
        try {
            FileOutputStream out = new FileOutputStream(outputFile);
            trans.setOut(out);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        try {
            trans.translate(cfgBuilder.getSections());
            trans.run(outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testIf() {
        String filePath = "src/test/resources/psi/if.template";
        String outputFile = filePath.substring(0, filePath.length() - 9) + ".psi";

        CFGBuilder cfgBuilder = new CFGBuilder(filePath, outputFile, false);
        PsiTranslator trans = new PsiTranslator();
        try {
            FileOutputStream out = new FileOutputStream(outputFile);
            trans.setOut(out);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        try {
            trans.translate(cfgBuilder.getSections());
            trans.run(outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testStan(){
        String filePath= "src/test/resources/stan2237.template";
        String outputFile = filePath.substring(0, filePath.length()-9) + ".psi";

        CFGBuilder cfgBuilder = new CFGBuilder(filePath, outputFile, false);
        PsiTranslator trans = new PsiTranslator();
        trans.setOut(System.out);
        try {
            trans.translate(cfgBuilder.getSections());
            trans.run(outputFile);
        } catch (Exception e){
            e.printStackTrace();

        }

    }
}

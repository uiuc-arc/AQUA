package tests;

import grammar.cfg.CFGBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import translators.PsiMatheTranslator;
import translators.PsiTranslator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

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
            Pair results = trans.run(outputFile);
            Assert.assertTrue(results.getRight().toString().length() == 0);
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
                    Pair results = trans.run(outputFile);
                    Assert.assertTrue(results.getRight().toString().length() == 0);

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
                    Pair results = trans.run(outputFile);
                    Assert.assertTrue(results.getRight().toString().length() == 0);

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
            // Pair results = trans.run(outputFile);
            // Assert.assertTrue(results.getRight().toString().length() == 0);
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
            Pair results = trans.run(outputFile);
            Assert.assertTrue(results.getRight().toString().length() == 0);
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
            Pair results = trans.run(outputFile);
            Assert.assertTrue(results.getRight().toString().length() == 0);
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
            Pair results = trans.run(outputFile);
            Assert.assertTrue(results.getRight().toString().length() == 0);
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
            Pair results = trans.run(outputFile);
            Assert.assertTrue(results.getRight().toString().length() == 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testHiv() {
        File folder = new File("../PPVM/autotemp/newtrans0418/");
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> restFiles=new ArrayList<>();
        for (File orgProgDir : listOfFiles) {
            if (!orgProgDir.isDirectory()) continue;
            if ((orgProgDir.getName().contains("hiv") && (!orgProgDir.getName().contains("chr")))
                    || orgProgDir.getName().contains("radon")
                    || orgProgDir.getName().contains("electric")
                    || orgProgDir.getName().contains("flight_simulator_17.3"))
                continue;
            if (!orgProgDir.getName().contains("gauss_mix"))
                continue;
            System.out.println(orgProgDir.getAbsolutePath() + "/" + orgProgDir.getName() + ".template");
            String filePath = (orgProgDir.getAbsolutePath() + "/" + orgProgDir.getName() + ".template");
            String outputFile = filePath.substring(0, filePath.length() - 9) + "_Org.psi";

            CFGBuilder cfgBuilder = new CFGBuilder(filePath, outputFile, false);
            PsiMatheTranslator trans = new PsiMatheTranslator();
            trans.setPath(filePath.replaceAll("/[a-zA-Z_0-9\\.]+\\.template", ""));
            try {
                FileOutputStream out = new FileOutputStream(outputFile);
                trans.setOut(out);
                String MatheOutputFile = filePath.substring(0, filePath.length() - 9) + "_analysis.m";
                FileOutputStream MatheOut = new FileOutputStream(MatheOutputFile);
                trans.setMatheOut(MatheOut);
                trans.translate(cfgBuilder.getSections());
                out.close();
            } catch (Exception e) {
                restFiles.add(orgProgDir.getName());
                e.printStackTrace();
            }
        }
        System.out.println(restFiles);
    }
}

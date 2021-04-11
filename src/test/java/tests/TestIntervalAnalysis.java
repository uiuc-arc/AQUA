package tests;

import grammar.analyses.*;
import grammar.cfg.*;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import translators.PyroTranslator;
import translators.Stan2IRTranslator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class TestIntervalAnalysis {


    //Tests Gen and Kill sets for a bigger program
    @Test
    //@Ignore
    public void Test5() throws IOException {

        String localDir = "/Users/zixin/Documents/uiuc/fall20/analysis/analysis_progs/progs/all/";
        String[] tt = new String[]{""}; // ,"_robust_student","_robust_reparam","_robust_reweight"}; // "",
        for (String ttt: tt)
            AnalysisRunner.analyzeProgram(localDir, "anova_radon_nopred" + ttt, "61");
        // gauss_mix_asym_prior

    }


    @Test
    public void Test4() throws IOException {
        String localDir = "/Users/zixin/Documents/uiuc/fall20/analysis/analysis_progs/progs/psi/";
        AnalysisRunner.analyzeTemplate(localDir, "radar_query2.template", "61");

    }

    @Test
    public void testPyroCond(){
        String stanfile = "src/test/resources/stan/stan1610.stan";
        String standata = "src/test/resources/stan/stan1610.data";
        Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator(stanfile, standata);
        String tempFileName = stanfile.replace(".stan", "");
        String templateCode = stan2IRTranslator.getCode();
        File tempfile = null;
        try {
            tempfile = File.createTempFile(tempFileName, ".template");
            FileUtils.writeStringToFile(tempfile, templateCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        CFGBuilder cfgBuilder = new CFGBuilder(tempfile.getAbsolutePath(), null, false);
        PyroTranslator pyroTranslator = new PyroTranslator();
        try {
            pyroTranslator.translate(cfgBuilder.getSections());
            System.out.println(pyroTranslator.getCode());
            //Pair results = pyroTranslator.run();
            //Assert.assertTrue(results.getRight().toString().length() == 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ///

    @Test
    public void Test7() throws IOException {
        File folder = new File("/Users/zixin/Documents/uiuc/fall20/analysis/analysis_progs/progs/org/");
        File[] listOfFiles = folder.listFiles();
        String targetOrgDir = "../PPVM/autotemp/newtrans1114/";
        //TODO: check sshfs mount; before run ./patch_vector.sh; after finish run ./patch_simplex.sh

        int i = 0;
        ArrayList<String> restFiles=new ArrayList<>();
        for (File orgProgDir : listOfFiles) {
            if (orgProgDir.isDirectory()) {
                String stanfile = "/Users/zixin/Documents/uiuc/fall19/are/PPVM/autotemp/trans" + "/" + orgProgDir.getName() + "/" + orgProgDir.getName() + ".stan";
                String standata = orgProgDir.getAbsolutePath() + "/" + orgProgDir.getName() + ".data.R";
                System.out.println(orgProgDir.getName());
                Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator(stanfile, standata);
                String templateCode = stan2IRTranslator.getCode();
                try (PrintWriter out = new PrintWriter(targetOrgDir + orgProgDir.getName() + ".template")) {
                    out.println(templateCode);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    @Test
    public void Test6() throws IOException {
        String stanfile = "src/test/resources/stan/stan1610.stan";
        String standata = "src/test/resources/stan/stan1610.data";
        Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator(stanfile, standata);
        String tempFileName = stanfile.replace(".stan", "");
        String templateCode = stan2IRTranslator.getCode();
        // System.out.println("========Stan Code to Template=======");
        // System.out.println(templateCode);
        File tempfile = File.createTempFile(tempFileName, ".template");
        FileUtils.writeStringToFile(tempfile, templateCode);
        CFGBuilder cfgBuilder = new CFGBuilder(tempfile.getAbsolutePath(), null);
        Set<BasicBlock> CFGVertexSet = cfgBuilder.getGraph().vertexSet();
        for (BasicBlock bb: CFGVertexSet)
            for (Statement ss : bb.getStatements())
                System.out.println(ss.statement.toString());
        // IntervalAnalysis.forwardAnalysis(CFG);
    }

    @Test
    public void TestNd4j() {
        Nd4j.setDataType(DataType.DOUBLE);
        System.out.println("==========");
        INDArray newarray;
        newarray = Nd4j.arange(0, 10, 2).reshape(new long[]{1,5});
        System.out.println(newarray.putScalar(2, 100));
        System.out.println(newarray);



    }

}

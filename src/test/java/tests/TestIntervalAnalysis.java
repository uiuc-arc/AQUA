package tests;

import grammar.AST;
import grammar.analyses.*;
import grammar.cfg.*;
import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Broadcast;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import translators.Stan2IRTranslator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static grammar.analyses.Utilities.printStamentAttributes;

public class TestIntervalAnalysis {


    //Tests Gen and Kill sets for a bigger program
    @Test
    //@Ignore
    public void Test5() throws IOException {

        String localDir = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/";
        AnalysisRunner.analyzeProgram(localDir, "unemployment_t");

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
        System.out.println("==========");
        INDArray[] newarray = new INDArray[2];
        newarray[0] = Nd4j.arange(36).reshape(2,3,6);
        System.out.println(Transforms.pow(newarray[0],2).subi(10));
        System.out.println(newarray[0]);

    }

}

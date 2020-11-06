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
import translators.Stan2IRTranslator;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static grammar.analyses.Utilities.printStamentAttributes;

public class TestIntervalAnalysis {


    //Tests Gen and Kill sets for a bigger program
    @Test
    //@Ignore
    public void Test5() throws IOException {
        // String stanfile = "src/test/resources/stan/unemployment.stan";
        // String standata = "src/test/resources/stan/unemployment.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/y_x/y_x.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/y_x/y_x.data.R";
       //  String stanfile = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/pilots/pilots.stan";
       //  String standata = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/pilots/pilots.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/electric_chr/electric_chr.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/electric_chr/electric_chr.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/lightspeed/lightspeed.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/lightspeed/lightspeed.data.R";
        String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/anova_radon_nopred_chr/anova_radon_nopred_chr.stan";
        String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/anova_radon_nopred_chr/anova_radon_nopred_chr.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/radon.pooling/radon.pooling.stan";
       //  String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/radon.pooling/radon.pooling.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/radon.1/radon.1.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/radon.1/radon.1.data.R";
        Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator(stanfile, standata);
        String tempFileName = stanfile.replace(".stan", "");
        String templateCode = stan2IRTranslator.getCode();
        System.out.println("========Stan Code to Template=======");
        System.out.println(templateCode);
        File tempfile = File.createTempFile(tempFileName, ".template");
        FileUtils.writeStringToFile(tempfile, templateCode);
        long startTime = System.nanoTime();
        CFGBuilder cfgBuilder = new CFGBuilder(tempfile.getAbsolutePath(), null);
        ArrayList<Section> CFG = cfgBuilder.getSections();
        IntervalAnalysis intervalAnalyzer = new IntervalAnalysis();
        int index=stanfile.lastIndexOf('/');
        intervalAnalyzer.setPath(stanfile.substring(0,index));
        intervalAnalyzer.forwardAnalysis(CFG);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1000000000.0;
        System.out.println("Analysis Time: " + duration);
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
        INDArray newarray = Nd4j.arange(5);
        System.out.println(Nd4j.empty().length());


    }

}

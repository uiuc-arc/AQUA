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

        String localDir = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/";
        AnalysisRunner.analyzeProgram(localDir, "unemployment_t");
        // String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/unemployment/unemployment.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/unemployment/unemployment.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/y_x/y_x.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/y_x/y_x.data.R";
       //  String stanfile = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/pilots/pilots.stan";
       //  String standata = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/pilots/pilots.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/electric_chr/electric_chr.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/electric_chr/electric_chr.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/lightspeed/lightspeed.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall19/are/aura_package/autotemp/org/lightspeed/lightspeed.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/anova_radon_nopred_chr/anova_radon_nopred_chr.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/anova_radon_nopred_chr/anova_radon_nopred_chr.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/gauss_mix_asym_prior/gauss_mix_asym_prior.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/gauss_mix_asym_prior/gauss_mix_asym_prior.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/gauss_mix_given_theta/gauss_mix_given_theta.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/gauss_mix_given_theta/gauss_mix_given_theta.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/gauss_mix_ordered_prior/gauss_mix_ordered_prior.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/gauss_mix_ordered_prior/gauss_mix_ordered_prior.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/radon.pooling/radon.pooling.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/radon.pooling/radon.pooling.sim.data.R";
        // String stanfile = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/radon.1/radon.1.stan";
        // String standata = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/radon.1/radon.1.data.R";
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
        INDArray newarray = Nd4j.arange(24).reshape(4,6);
        INDArray newarray2 = Nd4j.arange(24).reshape(4,6);
        newarray.slice(1).assign(Double.NaN);
        System.out.println(newarray.getDouble(8));
        double a = newarray.getDouble(8);
        System.out.println(Math.log(a));
        System.out.println(a*a/23.6);


    }

}

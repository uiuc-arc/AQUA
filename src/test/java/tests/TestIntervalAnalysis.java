package tests;

import grammar.AST;
import grammar.analyses.*;
import grammar.cfg.*;
import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.nd4j.linalg.api.ndarray.INDArray;
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
        String stanfile = "src/test/resources/stan/unemployment.stan";
        String standata = "src/test/resources/stan/unemployment.data.R";
        // String stanfile = "src/test/resources/stan/stan1610.stan";
        // String standata = "src/test/resources/stan/stan1610.data";
        // String stanfile = "src/test/resources/stan/radon.pooling.stan";
        // String standata = "src/test/resources/stan/radon.pooling.data.R";
        // String stanfile = "src/test/resources/stan/shots.stan";
        // String standata = "src/test/resources/stan/shots.data.R";
        Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator(stanfile, standata);
        String tempFileName = stanfile.replace(".stan", "");
        String templateCode = stan2IRTranslator.getCode();
        System.out.println("========Stan Code to Template=======");
        System.out.println(templateCode);
        File tempfile = File.createTempFile(tempFileName, ".template");
        FileUtils.writeStringToFile(tempfile, templateCode);
        CFGBuilder cfgBuilder = new CFGBuilder(tempfile.getAbsolutePath(), null);
        ArrayList<Section> CFG = cfgBuilder.getSections();
        IntervalAnalysis intervalAnalyzer = new IntervalAnalysis();
        intervalAnalyzer.forwardAnalysis(CFG);
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
        INDArray indArray = Nd4j.arange(6);
        INDArray indArray2 = Nd4j.arange(4);
        double [][] new_array = {{0.3,0.8,44},{0.4,0.5,55}};
        INDArray nnn = Nd4j.create(new_array);
        System.out.println(indArray.reshape(6,1).repeat(1,4));
        System.out.println(indArray2.reshape(1,4).repeat(0,6));
        System.out.println(indArray.reshape(6,1).repeat(1,4).mul(indArray2.reshape(1,4).repeat(0,6)));
        INDArray z = Nd4j.createUninitialized(6,4);
        System.out.println(nnn.reshape(2,3,1).broadcast(2,3,4).mul(indArray2.reshape(1,1,4).broadcast(2,3,4)));

    }

}

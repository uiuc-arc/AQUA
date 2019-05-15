package tests;

import grammar.analyses.*;
import grammar.AST;
import grammar.cfg.*;
import org.jgrapht.Graph;
import org.junit.Ignore;
import org.junit.Test;
import org.renjin.compiler.builtins.StaticMethodCall;

import java.util.*;

public class TestDataflow {

    //Tests the simple Join for the Reaching Definitions dataflow
    @Test
    //@Ignore
    public void Test1(){
        Set<Integer> locs1 = new HashSet<>();
        Set<Integer> locs2 = new HashSet<>();

        locs1.add(2);
        locs1.add(3);
        locs1.add(7);

        locs2.add(2);
        locs2.add(9);

        Map<String, Set<Integer>> varLocs1 = new HashMap<>();
        Map<String,Set<Integer>> varLocs2 = new HashMap<>();

        varLocs1.put("a",locs1);
        varLocs2.put("a",locs2);

        ReachingDefinitionsState s1 = new ReachingDefinitionsState(varLocs1);
        ReachingDefinitionsState s2 = new ReachingDefinitionsState(varLocs2);

        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();

        AbstractState joined = RD.Join(s1,s2);
        Set<Integer> varLocs = ((ReachingDefinitionsState) joined).VarLocations.get("a");

        Set<Integer> expected = new HashSet<>();
        expected.add(2); expected.add(3); expected.add(7); expected.add(9);

        assert (expected.equals(varLocs));

    }

    //tests the nFold Join
    @Test
    //@Ignore
    public void Test2(){
        Set<Integer> locs1 = new HashSet<>();
        Set<Integer> locs2 = new HashSet<>();
        Set<Integer> locs3 = new HashSet<>();

        locs1.add(2);
        locs1.add(3);
        locs1.add(7);

        locs2.add(2);
        locs2.add(9);

        locs3.add(21);

        Map<String, Set<Integer>> varLocs1 = new HashMap<>();
        Map<String,Set<Integer>> varLocs2 = new HashMap<>();
        Map<String,Set<Integer>> varLocs3 = new HashMap<>();

        varLocs1.put("a",locs1);
        varLocs2.put("a",locs2);
        varLocs3.put("a",locs3);

        ReachingDefinitionsState s1 = new ReachingDefinitionsState(varLocs1);
        ReachingDefinitionsState s2 = new ReachingDefinitionsState(varLocs2);
        ReachingDefinitionsState s3 = new ReachingDefinitionsState(varLocs3);
        ArrayList<AbstractState> collectedStates = new ArrayList<>();
        collectedStates.add(s1); collectedStates.add(s2); collectedStates.add(s3);


        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();
        AbstractState joined = RD.nFoldJoin(collectedStates);
        Set<Integer> varLocs = ((ReachingDefinitionsState) joined).VarLocations.get("a");

        Set<Integer> expected = new HashSet<>();
        expected.add(2); expected.add(3); expected.add(7); expected.add(9); expected.add(21);
        assert (expected.equals(varLocs));
    }

    //Test Dataflow/Reaching definitions on a small benchmark
    @Test
    //@Ignore
    public void Test3(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/test1.template", "src/test/resources/graph1.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        CFGAnalyzer.labelGenAndKill(CFG);
        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();
        Worklist.ForwardChaoticIteration(CFG,RD);
        Utilities.printDataflowInfo(CFG,"OUT");

        //check results
        AbstractState fact = null;
        for (BasicBlock bb : CFG.vertexSet()){
            for (Statement st : bb.getStatements()){
                //if it is the first assignment statement
                if (st.id == 1){
                     fact = st.dataflowFacts.get("OUT");
                     ReachingDefinitionsState rdFact = (ReachingDefinitionsState) fact;
                     Set<Integer> Expected = new HashSet<>(); Expected.add(1);
                     assert rdFact.VarLocations.get("x").equals(Expected);
                }
                //if it is the second assignment statement
                else if (st.id == 2){
                    fact = st.dataflowFacts.get("OUT");
                    ReachingDefinitionsState rdFact = (ReachingDefinitionsState) fact;
                    Set<Integer> Expected = new HashSet<>(); Expected.add(1);
                    assert rdFact.VarLocations.get("x").equals(Expected);
                    Expected.remove(1); Expected.add(2);
                    assert rdFact.VarLocations.get("y").equals(Expected);
                }
            }
        }
    }

    //Test Dataflow/Reaching definitions on a bigger benchmark (that contains a loop)
    @Test
    //@Ignore
    public void Test4(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/test3.template", "src/test/resources/graph3.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        CFGAnalyzer.labelGenAndKill(CFG);
        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();
        Worklist.ForwardChaoticIteration(CFG,RD);
        //Utilities.printDataflowInfo(CFG,"OUT");
        //check the for loop
        for (BasicBlock bb : CFG.vertexSet()){
            for (Statement st : bb.getStatements()){
                if (st.statement instanceof AST.ForLoop){
                    AbstractState dataflowFact = st.dataflowFacts.get("OUT");
                    assert (dataflowFact instanceof ReachingDefinitionsState);
                    Set<Integer> Expected = new HashSet<>();

                    //check that the set of variable locations for variable A is as expected
                    Expected.add(6);
                    assert ((ReachingDefinitionsState) dataflowFact).VarLocations.get("A").equals(Expected);

                    //check that the set of variable locations for variable B is as expected
                    Expected.clear();
                    Expected.add(7);    //var B is only defined at location 7 (no other possible)
                    assert ((ReachingDefinitionsState) dataflowFact).VarLocations.get("B").equals(Expected);

                    //check that the set of variable locations for variable y is as expected
                    Expected.clear();
                    Expected.add(12);
                    assert ((ReachingDefinitionsState) dataflowFact).VarLocations.get("y").equals(Expected);

                }
            }
        }
    }

    //The following programs don't have any asserts, the tests are simply to make sure that the dataflow doesn't die in
    //the middle or encounter something weird like a null value or the worklist not terminating.
    @Test
    //@Ignore
    public void Test5() {
        CFGBuilder builder = new CFGBuilder("src/test/resources/poisson.template", "src/test/resources/poisson.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        long startTime = System.nanoTime();
        CFGAnalyzer.labelGenAndKill(CFG);
        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();
        Worklist.ForwardChaoticIteration(CFG, RD);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/1000000;  //divide by 1000000 to get milliseconds.
        System.out.println(duration);
    }

    @Test
    public void Test6(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/linearregression.template", "src/test/resources/linearregression.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        long startTime = System.nanoTime();
        CFGAnalyzer.labelGenAndKill(CFG);
        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();
        Worklist.ForwardChaoticIteration(CFG, RD);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);//1000000;  //divide by 1000000 to get milliseconds.
        System.out.println(duration);
    }

    @Test
    //@Ignore
    public void Test7() {
        CFGBuilder builder = new CFGBuilder("src/test/resources/psi/probmods/ch02_generative_models/noisy_double.template", "src/test/resources/noisy_double.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        long startTime = System.nanoTime();
        CFGAnalyzer.labelGenAndKill(CFG);
        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();
        Worklist.ForwardChaoticIteration(CFG, RD);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/1000000;  //divide by 1000000 to get milliseconds.
        System.out.println(duration);
    }

    @Test
    //@Ignore
    public void Test8() {
        CFGBuilder builder = new CFGBuilder("src/test/resources/psi/for.template", "src/test/resources/forPSI.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        long startTime = System.nanoTime();
        CFGAnalyzer.labelGenAndKill(CFG);
        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();
        Worklist.ForwardChaoticIteration(CFG, RD);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/1000000;  //divide by 1000000 to get milliseconds.
        System.out.println(duration);
    }

    @Test
    //@Ignore
    public void Test9(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/psi/probmods/bayes_occams_razor.template", "src/test/resources/occams_razor.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        long startTime = System.nanoTime();
        CFGAnalyzer.labelGenAndKill(CFG);
        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();
        Worklist.ForwardChaoticIteration(CFG, RD);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/1000000;  //divide by 1000000 to get milliseconds.
        System.out.println(duration);
    }

    @Test
    //@Ignore
    public void Test10(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/leukfr0.template", "src/test/resources/leukfr0.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        long startTime = System.nanoTime();
        CFGAnalyzer.labelGenAndKill(CFG);
        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();
        Worklist.ForwardChaoticIteration(CFG, RD);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/1000000;  //divide by 1000000 to get milliseconds.
        System.out.println(duration);
    }


}

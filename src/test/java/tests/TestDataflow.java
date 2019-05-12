package tests;

import grammar.analyses.*;
import grammar.AST;
import grammar.cfg.*;
import org.jgrapht.Graph;
import org.junit.Test;
import org.renjin.compiler.builtins.StaticMethodCall;

import java.util.*;

public class TestDataflow {

    //Tests the simple Join for the Reaching Definitions dataflow
    @Test
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
    public void Test4(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/test3.template", "src/test/resources/graph3.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        CFGAnalyzer.labelGenAndKill(CFG);
        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();
        //CFGAnalyzer.initCFGLabels(CFG,RD);
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


}

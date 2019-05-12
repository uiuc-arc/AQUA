package tests;
import grammar.cfg.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import grammar.cfg.Edge;
import org.junit.Test;
import grammar.analyses.Utilities;
import grammar.analyses.*;

import java.util.*;

import static grammar.analyses.Utilities.printStamentAttributes;

public class TestCFGAnalysis {

    //Tests that the CFG works
    @Test
    public void Test1(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/test1.template", "src/test/resources/graph1.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        for (BasicBlock basicBlock : CFG.vertexSet()){
            ArrayList<Statement> bbStmts = basicBlock.getStatements();
            printStamentAttributes(bbStmts);
        }

    }

    //Tests Gen and Kill sets
    @Test
    public void Test2(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/test3.template", "src/test/resources/graph3.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        CFGAnalyzer.labelGenAndKill(CFG);
        for (BasicBlock bb : CFG.vertexSet()){
            for (Statement st : bb.getStatements()){
                java.lang.System.out.print("Statement is:  ");
                java.lang.System.out.println(st.statement);
                st.dataflowFacts.get("GEN").printAbsState();
                st.dataflowFacts.get("KILL").printAbsState();
                System.out.println("");

            }
        }

    }

    //This tests the function that gets all assigned variables in the program
    @Test
    public void Test3(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/test1.template", "src/test/resources/graph1.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        Set<String> allUsedVars = CFGAnalyzer.getAllVars(CFG);
        Set<String> Expected = new HashSet<>();
        Expected.add("m"); Expected.add("x"); Expected.add("y");
        assert (allUsedVars.equals(Expected));
    }


    //This tests the Transfer function method
    @Test
    public void Test4() {
        CFGBuilder builder = new CFGBuilder("src/test/resources/test1.template", "src/test/resources/graph1.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        CFGAnalyzer.labelGenAndKill(CFG);

        Set<Integer> locs1 = new HashSet<>();
        locs1.add(5); locs1.add(69); locs1.add(2); locs1.add(3);
        Map<String, Set<Integer>> varLocs1 = new HashMap<>();
        varLocs1.put("m",locs1);
        varLocs1.put("a",locs1);

        ReachingDefinitionsState s1 = new ReachingDefinitionsState(varLocs1);

        for (BasicBlock bb : CFG.vertexSet()){
            for (Statement st : bb.getStatements()){
                //should be the if(y) statement
                if (st.id == 3){
                    st.dataflowFacts.put("OUT",s1);
                }
            }
        }

        ReachingDefinitionsDomain RD = new ReachingDefinitionsDomain();

        for (BasicBlock bb : CFG.vertexSet()){
            for (Statement st : bb.getStatements()){
                //should be the if(y) statement
                if (st.id == 4){
                    AbstractState rdTest = RD.TransferFunction(st); //applies the transfer function
                    rdTest.printAbsState();
                }
            }
        }
    }


    //Tests Gen and Kill sets for a bigger program
    @Test
    public void Test5(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/test3.template", "src/test/resources/graph3.png");
        Graph<BasicBlock, Edge> CFG = builder.getGraph();
        CFGAnalyzer.labelGenAndKill(CFG);
        for (BasicBlock bb : CFG.vertexSet()){
            for (Statement st : bb.getStatements()){
                java.lang.System.out.print("Statement is:  ");
                java.lang.System.out.println(st.statement);
                st.dataflowFacts.get("GEN").printAbsState();
                st.dataflowFacts.get("KILL").printAbsState();
                System.out.println("");

            }
        }

    }

}

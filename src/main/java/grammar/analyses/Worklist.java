package grammar.analyses;

import org.jgrapht.Graph;
import org.renjin.gnur.api.S;
import java.lang.System.*;
import java.util.*;
import grammar.cfg.*;


public class Worklist {

    public static Queue<Statement> initWorkList(Collection<BasicBlock> basicBlocks){
        Queue<Statement> worklist = new LinkedList<>();

        for (BasicBlock basicBlock : basicBlocks){
            ArrayList<Statement> statements = basicBlock.getStatements();
            for (Statement s : statements){
                worklist.add(s);
            }
        }
        return worklist;
    }

    //Chaotic iteration to reach a fixpoint for Forward Dataflow Analysis
    public static void ForwardChaoticIteration(Graph<BasicBlock,Edge> origCFG, AbstractDomain AD) {
        Set<String> allUsedVars = CFGAnalyzer.getAllVars(origCFG);
        CFGAnalyzer.initCFGLabels(origCFG,AD);
        Queue<Statement> Worklist = initWorkList(origCFG.vertexSet());

        AbstractState before;
        AbstractState tmp;
        Statement currStatement;

        int count = 0;
        //iterate!
        while (!Worklist.isEmpty()) {
            count++;
            System.out.println("");
            currStatement = Worklist.remove();
            System.out.println(currStatement.statement);
            before = currStatement.dataflowFacts.get("OUT");
            //before.printAbsState();
            tmp = AD.TransferFunction(currStatement);
            tmp.printAbsState();
            //changed, meaning no fixed point reached
            //if (tmp != before){
            if (!AD.areEqual(tmp,before)){
                currStatement.dataflowFacts.put("OUT",tmp);
                ArrayList<Statement> succs = currStatement.getSuccStatements();
                for (Statement succ : succs){
                    Worklist.add(succ);
                }

            }
        }
    }

    //Chaotic iteration to reach a fixpoint for Backward Dataflow Analysis
    public static void BackwardChaoticIteration(Graph<BasicBlock,Edge> origCFG, AbstractDomain AD) {
        //return null;
    }

}


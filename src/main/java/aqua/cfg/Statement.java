package aqua.cfg;

import aqua.AST;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import aqua.analyses.AbstractState;

public class Statement {
    public AST.Statement statement;
    public aqua.cfg.BasicBlock parent;

    //maps a string such as "IN"/"OUT" or "GEN"/"KILL" to the dataflow fact
    public Map<String,AbstractState> dataflowFacts;
    public int id;
    static int classCount;

    public Statement(AST.Statement statement, BasicBlock basicBlock){
        this.statement = statement;
        this.parent = basicBlock;
        dataflowFacts = new HashMap<>();
        classCount++;
        this.id = classCount; //a simple way to associate a unique number/id to each statement object instance
    }

    public ArrayList<Statement> getPredStatements(){
        ArrayList<Statement> preds = new ArrayList<>();
        int ind = parent.statements.indexOf(this);  //get this statement's index

        //if it's not the first statement in the parent Basic Block, then we stay in the current Basic Block
        if (ind != 0 ){
            Statement predStmt = parent.statements.get(ind-1);  //get the statement that comes before in the same BB
            preds.add(predStmt);
            return preds;
        }
        //if it's the first statement in the basic block then we have to look at Predecessor Basic Blocks...
        else {
            ArrayList<aqua.cfg.BasicBlock> statementContainingPreds = parent.getStatementContainingPredBlocksAQ();
            if (!statementContainingPreds.isEmpty()) {
                for (BasicBlock bb : statementContainingPreds) {
                    preds.add(bb.getLastStatementAQ());
                }
                return preds;
            }
        }

        return preds;
    }

    public ArrayList<Statement> getSuccStatements(){
        ArrayList<Statement> succs = new ArrayList<>();
        int ind = parent.statements.indexOf(this);  //get this statement's index

        //if it is NOT the last statement in the basic block
        if (ind != this.parent.statements.size()-1){
            Statement succStmt = parent.statements.get(ind+1);
            succs.add(succStmt);
        }

        else {
            BasicBlock succBlock;
            Statement firstStmt;

            for (aqua.cfg.Edge e : this.parent.edges){
                succBlock = e.getTarget();
                if (!succBlock.statements.isEmpty()){
                    firstStmt = succBlock.statements.get(0);
                    succs.add(firstStmt);
                }
            }
        }
        return succs;
    }

    public void printDataFlowFacts(String key){
        AbstractState fact = this.dataflowFacts.get(key);
        fact.printAbsState();
    }
}

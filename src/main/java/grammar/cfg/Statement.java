package grammar.cfg;

import grammar.AST;
import java.util.ArrayList;


public class Statement {
    public AST.Statement statement;
    public BasicBlock parent;
    //public Map<String,AbstractState> dataflowFacts;    //maps a string such as "IN" or "OUT" or "GEN" or "KILL"
    public int id;
    static int classCount;

    public Statement(AST.Statement statement, BasicBlock basicBlock){
        this.statement = statement;
        this.parent = basicBlock;
        //dataflowFacts = new HashMap<>();
        classCount++;
        this.id = classCount;
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
            ArrayList<BasicBlock> statementContainingPreds = parent.getStatementContainingPredBlocks();
            if (!statementContainingPreds.isEmpty()) {
                for (BasicBlock bb : statementContainingPreds) {
                    preds.add(bb.getLastStatement());
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

            for (Edge e : this.parent.edges){
                succBlock = e.getTarget();
                if (!succBlock.statements.isEmpty()){
                    firstStmt = succBlock.statements.get(0);
                    succs.add(firstStmt);
                }
            }
        }
        return succs;
    }

//    public void printDataFlowFacts(String key){
//        AbstractState fact = this.dataflowFacts.get(key);
//        fact.printAbsState();
//    }
}

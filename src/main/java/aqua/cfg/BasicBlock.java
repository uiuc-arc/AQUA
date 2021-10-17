package aqua.cfg;
import aqua.AST;
import aqua.analyses.GridState;
import grammar.cfg.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BasicBlock extends grammar.cfg.BasicBlock {
    public GridState dataflowFacts = null;
    public ArrayList<aqua.cfg.Statement> getStatementsAQ() {
        return statements;
    }

    public ArrayList<AST.Data> getDataAQ() {
        return data;
    }

    public ArrayList<AST.Query> getQueriesAQ() {
        return queries;
    }

    public aqua.cfg.Section getParentAQ() {
       return parent;
    }

    public void setParentAQ(aqua.cfg.Section section){
       this.parent = section;
    }

    public SymbolTable getSymbolTableAQ() {
        return symbolTable;
    }

    ArrayList<Statement> statements;
    ArrayList<AST.Data> data;
    ArrayList<AST.Query> queries;

    public Map<String, aqua.cfg.BasicBlock> getIncomingEdgesAQ() {
        return incomingEdges;
    }

    public Map<String, aqua.cfg.BasicBlock> getOutgoingEdgesAQ() {
        return outgoingEdges;

    }

    // public void addIncomingEdge(BasicBlock basicBlock, String label){
    //     this.incomingEdges.put(label, basicBlock);
    // }

    // public void addOutgoingEdge(BasicBlock basicBlock, String label){
    //     this.outgoingEdges.put(label, basicBlock);
    // }

    Map<String, aqua.cfg.BasicBlock> incomingEdges;
    Map<String, aqua.cfg.BasicBlock> outgoingEdges;

    // public ArrayList<aqua.cfg.Edge> getEdges() {
    //     return edges;
    // }

    ArrayList<aqua.cfg.Edge> edges;
    aqua.cfg.Section parent;
    grammar.cfg.SymbolTable symbolTable;


    private int id;

    public BasicBlock(){
        statements = new ArrayList<>();
        data = new ArrayList<>();
        queries = new ArrayList<>();
        edges = new ArrayList<>();
        symbolTable = new grammar.cfg.SymbolTable(this);
        incomingEdges = new HashMap<>();
        outgoingEdges = new HashMap<>();
    }

    public BasicBlock(int id, aqua.cfg.Section section){
        this();
        this.id = id;
        this.parent = section;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Block@"+this.id+"\n");
        for(AST.Data data:data){
            stringBuilder.append(data.toString() +"\n");
        }

        for(Statement statement:statements){
            stringBuilder.append(statement.statement.toString() +"\n");
        }

        for(AST.Query query:queries){
            stringBuilder.append(query.toString() +"\n");
        }
        return stringBuilder.toString();
    }

    public void addStatement(AST.Statement statement){
        Statement statement1 = new Statement(statement, this);
        this.statements.add(statement1);
    }

    public void addData(AST.Data data){
        this.data.add(data);
    }

    public void addQuery(AST.Query query){
        this.queries.add(query);
    }

    public void addEdge(aqua.cfg.Edge edge){
        this.edges.add(edge);
    }

    public int getId() {
        return this.id;
    }

    public int getNumStmts() {return this.statements.size();}

    //for this basic block, get the last statement in the array list of statements
    public aqua.cfg.Statement getLastStatementAQ(){
        if (!this.statements.isEmpty()){
            return this.statements.get(this.getNumStmts()-1);
        }
        return null;
    }

    //for a this basic block, get ALL the immediate predecessor blocks that actually contain statements
    //the purpose is to skip over the blank basic blocks added during the compilation process
    public ArrayList<aqua.cfg.BasicBlock> getStatementContainingPredBlocksAQ(){
        ArrayList<BasicBlock> preds = new ArrayList<>();
        //if the Basic block has no predecesors (it is the first in the CFG)
        if (this.getIncomingEdges().isEmpty()){
            ArrayList<BasicBlock> nullList = new ArrayList<>();
            return nullList;
        }
        else {
            for (String key : this.getIncomingEdges().keySet()){
                aqua.cfg.BasicBlock pred = this.getIncomingEdgesAQ().get(key);

                //if the pred DOES have statements
                if (!pred.statements.isEmpty()){
                    preds.add(pred);
                }
                //recurse!
                else {
                    preds.addAll(pred.getStatementContainingPredBlocksAQ());
                }
            }
        }
        return preds;
    }
}

package grammar.cfg;
import grammar.AST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BasicBlock {
    public ArrayList<Statement> getStatements() {
        return statements;
    }

    public ArrayList<AST.Data> getData() {
        return data;
    }

    public ArrayList<AST.Query> getQueries() {
        return queries;
    }

    public Section getParent() {
        return parent;
    }

    public void setParent(Section section){
        this.parent = section;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    ArrayList<Statement> statements;
    ArrayList<AST.Data> data;
    ArrayList<AST.Query> queries;

    public Map<String, BasicBlock> getIncomingEdges() {
        return incomingEdges;
    }

    public Map<String, BasicBlock> getOutgoingEdges() {
        return outgoingEdges;

    }

    public void addIncomingEdge(BasicBlock basicBlock, String label){
        this.incomingEdges.put(label, basicBlock);
    }

    public void addOutgoingEdge(BasicBlock basicBlock, String label){
        this.outgoingEdges.put(label, basicBlock);
    }

    Map<String, BasicBlock> incomingEdges;
    Map<String, BasicBlock> outgoingEdges;

    public ArrayList<Edge> getEdges() {
        return edges;
    }

    ArrayList<Edge> edges;
    Section parent;
    SymbolTable symbolTable;


    private int id;

    public BasicBlock(){
        statements = new ArrayList<>();
        data = new ArrayList<>();
        queries = new ArrayList<>();
        edges = new ArrayList<>();
        symbolTable = new SymbolTable(this);
        incomingEdges = new HashMap<>();
        outgoingEdges = new HashMap<>();
    }

    public BasicBlock(int id, Section section){
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

    public void addEdge(Edge edge){
        this.edges.add(edge);
    }

    public int getId() {
        return this.id;
    }

    public int getNumStmts() {return this.statements.size();}

    //for this basic block, get the last statement in the array list of statements
    public Statement getLastStatement(){
        if (!this.statements.isEmpty()){
            return this.statements.get(this.getNumStmts()-1);
        }
        return null;
    }

    //for a this basic block, get ALL the immediate predecessor blocks that actually contain statements
    //the purpose is to skip over the blank basic blocks added during the compilation process
    public ArrayList<BasicBlock> getStatementContainingPredBlocks(){
        ArrayList<BasicBlock> preds = new ArrayList<>();
        //if the Basic block has no predecesors (it is the first in the CFG)
        if (this.getIncomingEdges().isEmpty()){
            ArrayList<BasicBlock> nullList = new ArrayList<>();
            return nullList;
        }
        else {
            for (String key : this.getIncomingEdges().keySet()){
                BasicBlock pred = this.getIncomingEdges().get(key);

                //if the pred DOES have statements
                if (!pred.statements.isEmpty()){
                    preds.add(pred);
                }
                //recurse!
                else {
                    preds.addAll(pred.getStatementContainingPredBlocks());
                }
            }
        }
        return preds;
    }
}

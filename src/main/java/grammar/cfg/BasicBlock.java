package grammar.cfg;
import grammar.AST;

import java.util.ArrayList;

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

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    ArrayList<Statement> statements;
    ArrayList<AST.Data> data;
    ArrayList<AST.Query> queries;
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

    public int getId() {
        return this.id;
    }
}

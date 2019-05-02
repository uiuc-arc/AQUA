package grammar.cfg;
import grammar.AST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BasicBlock {
    ArrayList<AST.Statement> statements;
    ArrayList<AST.Data> data;
    ArrayList<AST.Query> queries;
    ArrayList<Edge> edges;
    private int id;

    public BasicBlock(){
        statements = new ArrayList<>();
        data = new ArrayList<>();
        queries = new ArrayList<>();
        edges = new ArrayList<>();
    }

    public BasicBlock(int id){
        this();
        this.id = id;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Block@"+this.id+"\n");
        for(AST.Data data:data){
            stringBuilder.append(data.toString() +"\n");
        }

        for(AST.Statement statement:statements){
            stringBuilder.append(statement.toString() +"\n");
        }

        for(AST.Query query:queries){
            stringBuilder.append(query.toString() +"\n");
        }

        return stringBuilder.toString();
    }

    public int getId() {
        return this.id;
    }
}

package main.java.cfg;

import main.java.AST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BasicBlock {
    ArrayList<AST.Statement> statements;
    ArrayList<AST.Data> data;
    ArrayList<AST.Query> queries;
    ArrayList<Edge> edges;

    public BasicBlock(){
        statements = new ArrayList<>();
        data = new ArrayList<>();
        queries = new ArrayList<>();
        edges = new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
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
}

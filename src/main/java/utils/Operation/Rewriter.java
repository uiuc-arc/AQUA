package utils.Operation;

import grammar.AST;
import grammar.cfg.BasicBlock;
import grammar.cfg.Edge;
import grammar.cfg.Section;
import grammar.cfg.Statement;

import java.util.*;

public class Rewriter {
    Map<Statement, Record> rewriteRecords;
    ArrayList<Section> sections;

    public Rewriter(ArrayList<Section> sections){
        this.sections = sections;
        this.rewriteRecords = new HashMap<>();
    }

    public void delete(Statement node){
        Record record = new Record();
        record.operationType = OperationType.DELETE;
        record.source = node;
        this.rewriteRecords.put(node, record);
    }

    public void replace(Statement source, Statement target){
        Record record = new Record();
        record.operationType = OperationType.REPLACE;
        record.source = source;
        record.target = target;
        this.rewriteRecords.put(source, record);
    }

    public ArrayList<Section> rewrite(){
        ArrayList<Section> final_sections = new ArrayList<>();
        for(Section section: sections){
            Section section1 = new Section(section.sectionType, section.sectionName);
            final_sections.add(section1);
            for(BasicBlock basicBlock:section.basicBlocks){
                BasicBlock basicBlock1 = new BasicBlock(basicBlock.getId(), section1);
                section1.basicBlocks.add(basicBlock1);
                for(Statement statement:basicBlock.getStatements()){
                    if(this.rewriteRecords.containsKey(statement)){
                        if(this.rewriteRecords.get(statement).operationType == OperationType.DELETE){
                            continue;
                        }
                        else{
                            basicBlock1.addStatement((this.rewriteRecords.get(statement).target).statement);
                        }
                    }
                    else {
                        basicBlock1.addStatement(statement.statement);
                    }
                }
                for(AST.Data data:basicBlock.getData()){
                    basicBlock1.addData(data);
                }

                for(AST.Query query:basicBlock.getQueries()){
                    basicBlock1.addQuery(query);
                }

                for(Edge edge:basicBlock.getEdges()){
                    basicBlock1.addEdge(edge);
                }
            }
        }

        return final_sections;
    }
}

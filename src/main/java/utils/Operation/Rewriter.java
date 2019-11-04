package utils.Operation;

import grammar.AST;
import grammar.cfg.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import utils.Utils;

import java.util.*;

public class Rewriter {
    Graph<BasicBlock, Edge> graph;
    Map<Statement, Record> rewriteRecords;
    ArrayList<Section> sections;


    public Rewriter(ArrayList<Section> sections){
        this.sections = sections;
        this.rewriteRecords = new HashMap<>();
        graph = new DefaultDirectedGraph<>(Edge.class);
    }

    public void showGraph(String outputfile){
        Utils.showGraph(this.graph, outputfile);
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
        Map<BasicBlock, BasicBlock> basicBlockMapping = new HashMap<>();
        for(Section section: sections){
            Section section1 = new Section(section.sectionType, section.sectionName);
            final_sections.add(section1);

            for(BasicBlock basicBlock:section.basicBlocks){
                BasicBlock basicBlock1 = new BasicBlock(basicBlock.getId(), section1);
                copySymbolTable(basicBlock.getSymbolTable(), basicBlock1.getSymbolTable());

                section1.basicBlocks.add(basicBlock1);
                graph.addVertex(basicBlock1);
                basicBlockMapping.put(basicBlock, basicBlock1);

                for(Statement statement:basicBlock.getStatements()){
                    if(this.rewriteRecords.containsKey(statement)){
                        if(this.rewriteRecords.get(statement).operationType == OperationType.DELETE){
                            continue;
                        }
                        else{
                            if(this.rewriteRecords.get(statement).target.statement instanceof AST.ForLoop){

                                Statement target = this.rewriteRecords.get(statement).target;
                                // add new blocks
                                section1.basicBlocks.add(target.parent);
                                basicBlock1.getSymbolTable().fork(target.parent);

                                section1.basicBlocks.add(target.parent.getOutgoingEdges().get("true"));
                                target.parent.setParent(section1);
                                target.parent.getOutgoingEdges().get("true").setParent(section1);
                                target.parent.getSymbolTable().fork(target.parent.getOutgoingEdges().get("true"));
                                this.graph.addVertex(target.parent);
                                this.graph.addVertex(target.parent.getOutgoingEdges().get("true"));
                                basicBlockMapping.put(target.parent, target.parent);
                                basicBlockMapping.put(target.parent.getOutgoingEdges().get("true"), target.parent.getOutgoingEdges().get("true"));

                                // handle edges
                                Edge edge = new Edge(target.parent, null);
                                addEdge(basicBlock1, target.parent, edge);

                                //TODO: hack: get concrete edge instead of 0
                                Edge edge1 = new Edge(target.parent.getOutgoingEdges().get("true"), "true");
                                addEdge(basicBlock.getIncomingEdges().get(null), basicBlock.getIncomingEdges().get(null).getEdges().get(0), basicBlockMapping);
                                //addEdge(target.parent.getIncomingEdges().get("back"), target.parent, new Edge);
                                this.graph.addEdge(target.parent, target.parent.getOutgoingEdges().get("true"), edge1);
                                this.graph.addEdge(target.parent.getOutgoingEdges().get("true"), target.parent, new Edge(target.parent, "back"));


                                //create new block
                                BasicBlock basicBlock2 = new BasicBlock(basicBlock1.getId()+100, section1); //TODO: check ID generation!
                                target.parent.getSymbolTable().fork(basicBlock2);

                                section1.basicBlocks.add(basicBlock2);
                                basicBlockMapping.put(basicBlock, basicBlock2);
                                this.graph.addVertex(basicBlock2);

                                // new edges from loop to new basic block
                                Edge edge2 = new Edge(basicBlock2, "false");
                                addEdge(target.parent, basicBlock2, edge2);

                                basicBlock1 = basicBlock2;

                            }
                            else {
                                basicBlock1.addStatement((this.rewriteRecords.get(statement).target).statement);
                            }
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
            }
        }

        // add edges
        for(Section section:sections){
            for(BasicBlock basicBlock:section.basicBlocks){
                for(Edge edge:basicBlock.getEdges()){
                    addEdge(basicBlock, edge, basicBlockMapping);

                    basicBlockMapping.get(basicBlock).getSymbolTable().fork(basicBlockMapping.get(edge.getTarget()));

                }

            }
        }

        return final_sections;
    }

    private void addEdge(BasicBlock source, Edge edge, Map<BasicBlock, BasicBlock> basicBlockMap){
        Edge newedge = new Edge(basicBlockMap.get(edge.getTarget()), edge.getLabel());
//        if(basicBlockMap.get(source).getOutgoingEdges().containsKey(edge.getLabel())){
//            System.out.println("Skipping edge");
//            return;
//        }
        addEdge(basicBlockMap.get(source), basicBlockMap.get(edge.getTarget()), newedge);
    }

    private void addEdge(BasicBlock source, BasicBlock target, Edge edge){
        if(source.getOutgoingEdges().containsKey(edge.getLabel())){
            System.out.println("Skipping");
            return;
        }

        source.addEdge(edge);
        source.addOutgoingEdge(target, edge.getLabel());
        target.addIncomingEdge(source, edge.getLabel());
        this.graph.addEdge(source, target, edge);
    }

    private void copySymbolTable(SymbolTable source, SymbolTable target){
        for(Map.Entry<String, SymbolInfo> entry:source.getTable()){
            try {
                target.addEntry(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

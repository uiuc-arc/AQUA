package aqua.cfg;


import grammar.AST;
import grammar.Template3Lexer;
import grammar.Template3Parser;
import grammar.cfg.Edge;
import grammar.cfg.Section;
import grammar.cfg.SectionType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;


public class CFGBuilder extends grammar.cfg.CFGBuilder {

    ArrayList<Section> sections;
    Section curSection;
    BasicBlock curBasicBlock;
    Stack<Section> sectionStack;
    Stack<BasicBlock> basicBlocksStack;
    Graph<BasicBlock, Edge> graph;
    String outputfile;
    boolean showCFG;
    public Template3Parser parser;
    private int blockId = 1;

    private BasicBlock createBasicBlock(Section section){
        BasicBlock basicBlock = new BasicBlock(blockId, section);
        blockId++;
        graph.addVertex(basicBlock);
        section.basicBlocks.add(basicBlock);
        return basicBlock;
    }

    private void addEdge(BasicBlock from, BasicBlock to, String label){
        Edge edge = new Edge(to, label);
        from.getEdges().add(edge);
        to.getIncomingEdges().put(label, from);
        from.getOutgoingEdges().put(label, to);
        this.graph.addEdge(from, to, edge);
    }

    public aqua.cfg.BasicBlock buildBasicBlock(ArrayList<AST.Statement> statements, Section section, aqua.cfg.BasicBlock prevBasicBlock, String label){
        aqua.cfg.BasicBlock curBlock = prevBasicBlock;

        if(prevBasicBlock == null || prevBasicBlock.getStatements().size() > 0 || prevBasicBlock.getData().size() > 0 ){
            curBlock = createBasicBlock(section);
            // create new symbol table
            if(prevBasicBlock != null)
            {   prevBasicBlock.getSymbolTable().fork(curBlock);
                addEdge(prevBasicBlock, curBlock, label);
            }
        }

        for(AST.Statement statement:statements){
            AST.Annotation annotation = shiftSection(statement);
            if(annotation != null){
                AST.MarkerWrapper wrapper = (AST.MarkerWrapper) annotation.annotationValue;
                if(wrapper.marker == AST.Marker.Start){
                    Section newsection = createSection(SectionType.NAMEDSECTION, wrapper.id.id);
                    aqua.cfg.BasicBlock newBlock = createBasicBlock(newsection);
                    addEdge(curBlock, newBlock, null);
                    curBlock.getSymbolTable().fork(newBlock);
                    this.sectionStack.push(section);
                    section = newsection;
                    curBlock = newBlock;
                    System.out.println("Moved into section: "+wrapper.id.id);
               }
//                else{
//                    section = this.sectionStack.pop();
//                    BasicBlock newblock = createBasicBlock(section);
//                    addEdge(curBlock, newblock, null);
//                    curBlock = newblock;
//                    System.out.println("Changed section");
//                }
            }

            if(statement instanceof AST.IfStmt){
                AST.IfStmt ifStmt = (AST.IfStmt) statement;
                curBlock.addStatement(ifStmt);
                aqua.cfg.BasicBlock trueBlock = buildBasicBlock(ifStmt.trueBlock.statements, section, curBlock, "true");
                aqua.cfg.BasicBlock newblock = createBasicBlock(section);
                if(ifStmt.elseBlock != null){
                    aqua.cfg.BasicBlock falseBlock = buildBasicBlock(ifStmt.elseBlock.statements, section, curBlock, "false");
                    addEdge(falseBlock, newblock, "meetF");
                }
                else{
                    addEdge(curBlock, newblock, "false");
                }

                addEdge(trueBlock, newblock, "meetT");

//                curBlock.edges.add(new Edge(trueBlock, "true"));
//                curBlock.edges.add(new Edge(falseBlock, "false"));

                // start new basic block


                // add new edges



//                // fork new symbol table
//                curBlock.getSymbolTable().fork(newblock);

                // change current block
                curBlock = newblock;
            }
            else if(statement instanceof AST.ForLoop){
                AST.ForLoop forLoop = (AST.ForLoop) statement;
                aqua.cfg.BasicBlock loop_condition_block = curBlock;
                if(curBlock.getStatements().size() > 0){
                    loop_condition_block = createBasicBlock(section);
                    curBlock.getSymbolTable().fork(loop_condition_block);
                    addEdge(curBlock, loop_condition_block, null);

                }
                //BasicBlock loop_condition_block = createBasicBlock(section);
                //BasicBlock loop_condition_block = curBlock;
                loop_condition_block.addStatement(forLoop);

                //curBlock.statements.add(forLoop);
                aqua.cfg.BasicBlock loopbody = buildBasicBlock(forLoop.block.statements, section, loop_condition_block, "true");
                aqua.cfg.BasicBlock newblock;
//                if(loopbody.statements.size() != 0){
//                    newblock = createBasicBlock(section);
//                }
//                else{
//                    newblock = loopbody;
//                }

                newblock = createBasicBlock(section);
                //addEdge(loopbody, newblock, null);
                addEdge(loop_condition_block, newblock, "false");
                addEdge(loopbody, loop_condition_block, "back");
                forLoop.BBloopBody = loopbody;
                forLoop.BBloopCond = loop_condition_block;

                loop_condition_block.getSymbolTable().fork(newblock);


                curBlock = newblock;
            }
            else if(statement instanceof AST.Decl){
                try {
                    curBlock.getSymbolTable().addEntry((AST.Decl) statement, false);
                    curBlock.addStatement(statement);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else{
                curBlock.addStatement(statement);
            }

            if(annotation != null){
                AST.MarkerWrapper wrapper = (AST.MarkerWrapper) annotation.annotationValue;
                if(wrapper.marker == AST.Marker.End){
                    section = this.sectionStack.pop();
                    aqua.cfg.BasicBlock newblock = createBasicBlock(section);
                    addEdge(curBlock, newblock, null);
                    curBlock.getSymbolTable().fork(newblock);
                    curBlock = newblock;
                    System.out.println("Moved out of section: " + wrapper.id.id);
                }
            }

        }

        //returns last block
        return curBlock;
    }

    private AST.Annotation shiftSection(AST.Statement statement){
        if(statement.annotations != null && statement.annotations.size() > 0){
            for(AST.Annotation annotation:statement.annotations){
                if(annotation.annotationType == AST.AnnotationType.Blk){
                    return annotation;
                }
            }
        }

        return null;
    }

    private Section createSection(SectionType sectionType, String name){
        Section section = new Section(sectionType, name);
        this.sections.add(section);
        return section;
    }
}

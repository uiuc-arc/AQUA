package aqua.cfg;

import aqua.AST;
import aqua.Template3Parser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import utils.CommonUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;
import grammar.cfg.*;


public class CFGBuilder{
    public ArrayList<Section> getSections() {
        return sections;
    }

    ArrayList<Section> sections;
    Section curSection;
    aqua.cfg.BasicBlock curBasicBlock;
    Stack<Section> sectionStack;
    Stack<aqua.cfg.BasicBlock> basicBlocksStack;
    Graph<aqua.cfg.BasicBlock, aqua.cfg.Edge> graph;
    String outputfile;
    boolean showCFG;
    public Template3Parser parser;

    private int blockId = 1;

    public CFGBuilder(String filename, String outputfile, boolean showCFG){
        this.graph = new DefaultDirectedGraph<>(aqua.cfg.Edge.class);
        this.sections = new ArrayList<>();
        this.sectionStack = new Stack<>();
        this.basicBlocksStack = new Stack<>();
        this.showCFG = showCFG;

        if(outputfile != null)
            this.outputfile = outputfile;
        else
            this.outputfile = "src/test/resources/" + filename.split("/")[filename.split("/").length - 1].split("\\.")[0] + ".png";

        createCFG(filename);
    }

    public CFGBuilder(String filename, String outputfile){
        this(filename, outputfile, true);
    }

    private Section createSection(){
        Section section = new Section();
        this.sections.add(section);
        return section;
    }

    private void addEdge(aqua.cfg.BasicBlock from, aqua.cfg.BasicBlock to, String label){
        aqua.cfg.Edge edge = new aqua.cfg.Edge(to, label);
        from.edges.add(edge);
        to.getIncomingEdges().put(label, from);
        from.getOutgoingEdges().put(label, to);
        this.graph.addEdge(from, to, edge);

    }

    private Section createSection(SectionType sectionType){
        Section section = new Section(sectionType);
        this.sections.add(section);
        return section;
    }

    private Section createSection(SectionType sectionType, String name){
        Section section = new Section(sectionType, name);
        this.sections.add(section);
        return section;
    }

    public void createCFG(String filename){
        this.parser = getParser(filename);
        AST.Program program = parser.template().value;
        aqua.cfg.BasicBlock basicBlock = null;
        Section section = null;
        if(program.data.size() > 0) {
            section = createSection(SectionType.DATA, "data");
            basicBlock = createBasicBlock(section);

            for (AST.Data data : program.data) {
                basicBlock.data.add(data);
                try {
                    basicBlock.getSymbolTable().addEntry(data.decl, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if(program.statements.size() > 0) {
            section = createSection(SectionType.FUNCTION, "main");

            basicBlock = buildBasicBlock(program.statements, section, basicBlock, null);
        }

        section = createSection(SectionType.QUERIES, "queries");
        aqua.cfg.BasicBlock queriesBasicBlock = createBasicBlock(section);
        addEdge(basicBlock, queriesBasicBlock, null);
        for(AST.Query query:program.queries){
            queriesBasicBlock.queries.add(query);
        }
        /*
        for(Section s:this.sections){
            System.out.print("Section  " + s.sectionName + ": ");
            for(BasicBlock b:s.basicBlocks){
                System.out.print(b.getId() + ",");
            }
            System.out.println();
        }
        */

        if(this.showCFG) {
            CommonUtils.showGraph(this.graph, this.outputfile);
        }
    }

    public aqua.cfg.BasicBlock buildBasicBlock(ArrayList<AST.Statement> statements, Section section, aqua.cfg.BasicBlock prevBasicBlock, String label){
        aqua.cfg.BasicBlock curBlock = prevBasicBlock;

        if(prevBasicBlock == null || prevBasicBlock.statements.size() > 0 || prevBasicBlock.data.size() > 0 ){
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
                if(curBlock.statements.size() > 0){
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

    public Template3Parser getParser(String filename){
        try{
            aqua.Template3Lexer template3Lexer = new aqua.Template3Lexer(CharStreams.fromFileName(filename));
            CommonTokenStream tokens = new CommonTokenStream(template3Lexer);
            Template3Parser parser = new Template3Parser(tokens);
            return parser;
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }

        return null;
    }




    private aqua.cfg.BasicBlock createBasicBlock(Section section){
        aqua.cfg.BasicBlock basicBlock = new aqua.cfg.BasicBlock(blockId, section);
        blockId++;
        graph.addVertex(basicBlock);
        section.basicBlocks.add(basicBlock);
        return basicBlock;
    }


    //gets the graph attribute of this class
    public Graph<aqua.cfg.BasicBlock, aqua.cfg.Edge> getGraph(){
        return this.graph;
    }
}

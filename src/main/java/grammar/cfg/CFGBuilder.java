package grammar.cfg;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.util.mxCellRenderer;
import grammar.AST;
import grammar.Template3Lexer;
import grammar.Template3Parser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;


public class CFGBuilder{
    public ArrayList<Section> getSections() {
        return sections;
    }

    ArrayList<Section> sections;
    Section curSection;
    BasicBlock curBasicBlock;
    Stack<Section> sectionStack;
    Stack<BasicBlock> basicBlocksStack;
    Graph<BasicBlock, Edge> graph;
    String outputfile;
    boolean showCFG;

    private int blockId = 1;

    public CFGBuilder(String filename, String outputfile, boolean showCFG){
        this.graph = new DefaultDirectedGraph<>(Edge.class);
        this.sections = new ArrayList<>();
        this.sectionStack = new Stack<>();
        this.basicBlocksStack = new Stack<>();
        this.showCFG = showCFG;

//        this.curSection = new Section(SectionType.FUNCTION, "main");
//        this.curBasicBlock = createBasicBlock();
        //buildCFG(filename);
        if(outputfile != null)
            this.outputfile = outputfile;
        else
            this.outputfile = "src/test/resources/graph.png";

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

    private void addEdge(BasicBlock from, BasicBlock to, String label){
        Edge edge = new Edge(to, label);
        from.edges.add(edge);
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
        Template3Parser parser = getParser(filename);
        AST.Program program = parser.template().value;
        BasicBlock basicBlock = null;
        Section section = null;
        if(program.data.size() > 0) {
            section = createSection(SectionType.DATA, "data");
            basicBlock = createBasicBlock(section);

            for (AST.Data data : program.data) {
                basicBlock.data.add(data);
            }
        }

        if(program.statements.size() > 0) {
            section = createSection(SectionType.FUNCTION, "main");
            basicBlock = buildBasicBlock(program.statements, section, basicBlock, null);
        }

        section = createSection(SectionType.QUERIES, "queries");
        BasicBlock queriesBasicBlock = createBasicBlock(section);
        addEdge(basicBlock, queriesBasicBlock, null);
        for(AST.Query query:program.queries){
            queriesBasicBlock.queries.add(query);
        }
        for(Section s:this.sections){
            System.out.print("Section  " + s.sectionName + ": ");
            for(BasicBlock b:s.basicBlocks){
                System.out.print(b.getId() + ",");
            }
            System.out.println();
        }

        if(this.showCFG) {
            showGraph();
        }
    }

    public BasicBlock buildBasicBlock(ArrayList<AST.Statement> statements, Section section, BasicBlock prevBasicBlock, String label){
        BasicBlock curBlock = createBasicBlock(section);

        if(prevBasicBlock != null){
            addEdge(prevBasicBlock, curBlock, label);
        }

        for(AST.Statement statement:statements){
            AST.Annotation annotation = shiftSection(statement);
            if(annotation != null){
                AST.MarkerWrapper wrapper = (AST.MarkerWrapper) annotation.annotationValue;
                if(wrapper.marker == AST.Marker.Start){
                    Section newsection = createSection(SectionType.NAMEDSECTION, wrapper.id.id);
                    BasicBlock newBlock = createBasicBlock(newsection);
                    addEdge(curBlock, newBlock, null);

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
                BasicBlock trueBlock = buildBasicBlock(ifStmt.trueBlock.statements, section, curBlock, "true");

                BasicBlock falseBlock = buildBasicBlock(ifStmt.elseBlock.statements, section, curBlock, "false");
//                curBlock.edges.add(new Edge(trueBlock, "true"));
//                curBlock.edges.add(new Edge(falseBlock, "false"));

                // start new basic block
                BasicBlock newblock = createBasicBlock(section);

                // add new edges
                addEdge(trueBlock, newblock, null);
                addEdge(falseBlock, newblock, null);

                // change current block
                curBlock = newblock;
            }
            else if(statement instanceof AST.ForLoop){
                AST.ForLoop forLoop = (AST.ForLoop) statement;
                BasicBlock loop_condition_block = createBasicBlock(section);
                loop_condition_block.addStatement(forLoop);

                addEdge(curBlock, loop_condition_block, null);

                //curBlock.statements.add(forLoop);
                BasicBlock loopbody = buildBasicBlock(forLoop.block.statements, section, loop_condition_block, "true");
                BasicBlock newblock = createBasicBlock(section);
                //addEdge(loopbody, newblock, null);
                addEdge(loop_condition_block, newblock, "false");
                addEdge(loopbody, loop_condition_block, "back");
                curBlock = newblock;
            }
            else{
                curBlock.addStatement(statement);
            }

            if(annotation != null){
                AST.MarkerWrapper wrapper = (AST.MarkerWrapper) annotation.annotationValue;
                if(wrapper.marker == AST.Marker.End){
                    section = this.sectionStack.pop();
                    BasicBlock newblock = createBasicBlock(section);
                    addEdge(curBlock, newblock, null);
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
            Template3Lexer template3Lexer = new Template3Lexer(CharStreams.fromFileName(filename));
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

//    public void buildCFG(String filename){
//            Template3Parser parser = getParser(filename);
//            ParseTreeWalker walker = new ParseTreeWalker();
//            walker.walk(this, parser.template());
//            showGraph();
//    }

    private void showGraph(){
        JGraphXAdapter<BasicBlock, Edge> graphXAdapter = new JGraphXAdapter<BasicBlock, Edge>(this.graph);
        mxIGraphLayout layout = new mxHierarchicalLayout(graphXAdapter);// new mxCircleLayout(graphXAdapter);
        layout.execute(graphXAdapter.getDefaultParent());
        BufferedImage image =
                mxCellRenderer.createBufferedImage(graphXAdapter, null, 2, Color.WHITE, true, null);
        File imgFile = new File(this.outputfile);
        try {
            ImageIO.write(image, "PNG", imgFile);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //private JGraphXAdapter<String, DefaultEdge> jgxAdapter;
    }


    private BasicBlock createBasicBlock(Section section){
        BasicBlock basicBlock = new BasicBlock(blockId, section);
        blockId++;
        graph.addVertex(basicBlock);
        section.basicBlocks.add(basicBlock);
        return basicBlock;
    }
}

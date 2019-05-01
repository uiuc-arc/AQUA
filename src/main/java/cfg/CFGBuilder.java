package main.java.cfg;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.util.mxCellRenderer;
import main.java.AST;
import main.java.Template3BaseListener;
import main.java.Template3Lexer;
import main.java.Template3Parser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;


public class CFGBuilder extends Template3BaseListener {
    ArrayList<Section> sections;
    Section curSection;
    BasicBlock curBasicBlock;
    Stack<Section> sectionStack;
    Stack<BasicBlock> basicBlocksStack;
    Graph<BasicBlock, Edge> graph;

    public CFGBuilder(String filename){
        this.graph = new SimpleGraph<>(Edge.class);
        this.sections = new ArrayList<>();
        this.sectionStack = new Stack<>();
        this.basicBlocksStack = new Stack<>();

//        this.curSection = new Section(SectionType.FUNCTION, "main");
//        this.curBasicBlock = createBasicBlock();
        //buildCFG(filename);
        createCFG(filename);
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
            section = createSection(SectionType.DATA);
            basicBlock = createBasicBlock(section);

            for (AST.Data data : program.data) {
                basicBlock.data.add(data);
            }
        }

        if(program.statements.size() > 0) {
            section = createSection(SectionType.FUNCTION, "main");
            basicBlock = buildBasicBlock(program.statements, section, basicBlock, null);
        }

        section = createSection(SectionType.QUERIES);
        BasicBlock queriesBasicBlock = createBasicBlock(section);
        addEdge(basicBlock, queriesBasicBlock, null);
        for(AST.Query query:program.queries){
            queriesBasicBlock.queries.add(query);
        }

        showGraph();
    }

    public BasicBlock buildBasicBlock(ArrayList<AST.Statement> statements, Section section, BasicBlock prevBasicBlock, String label){
        BasicBlock curBlock = createBasicBlock(section);

        if(prevBasicBlock != null){
            addEdge(prevBasicBlock, curBlock, label);
        }

        for(AST.Statement statement:statements){
            if(statement instanceof AST.IfStmt){
                AST.IfStmt ifStmt = (AST.IfStmt) statement;
                curBlock.statements.add(ifStmt);
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
            else{
                curBlock.statements.add(statement);
            }
        }

        //returns last block

        return curBlock;
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

    public void buildCFG(String filename){
            Template3Parser parser = getParser(filename);
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(this, parser.template());
            showGraph();
    }

    private void showGraph(){
        JGraphXAdapter<BasicBlock, Edge> graphXAdapter = new JGraphXAdapter<BasicBlock, Edge>(this.graph);
        mxIGraphLayout layout = new mxHierarchicalLayout(graphXAdapter);// new mxCircleLayout(graphXAdapter);
        layout.execute(graphXAdapter.getDefaultParent());
        BufferedImage image =
                mxCellRenderer.createBufferedImage(graphXAdapter, null, 2, Color.WHITE, true, null);
        File imgFile = new File("src/test/resources/graph.png");
        try {
            ImageIO.write(image, "PNG", imgFile);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //private JGraphXAdapter<String, DefaultEdge> jgxAdapter;
    }

    private BasicBlock createBasicBlock(){
        BasicBlock basicBlock = new BasicBlock();
        graph.addVertex(basicBlock);
        return basicBlock;
    }

    private BasicBlock createBasicBlock(Section section){
        BasicBlock basicBlock = createBasicBlock();
        section.basicBlocks.add(basicBlock);
        return basicBlock;
    }

//    // data
//    @Override
//    public void enterData(Template3Parser.DataContext ctx) {
//        if(this.curSection.sectionType != SectionType.DATA){
//            this.sectionStack.push(this.curSection);
//            this.basicBlocksStack.push(this.curBasicBlock);
//        }
//
//        curSection = new Section(SectionType.DATA);
//        curBasicBlock = createBasicBlock();
//        this.sections.add(curSection);
//        curSection.basicBlocks.add(curBasicBlock);
//    }
//
//    @Override
//    public void enterArray(Template3Parser.ArrayContext ctx) {
//        curBasicBlock.data.add(ctx.value);
//    }
//
//    @Override
//    public void enterVector(Template3Parser.VectorContext ctx) {
//        curBasicBlock.data.add(ctx.value);
//    }
//
//    // queries
//
//    @Override
//    public void enterQuery(Template3Parser.QueryContext ctx) {
//        if(curSection.sectionType != SectionType.QUERIES) {
//            this.sectionStack.push(this.curSection);
//            this.basicBlocksStack.push(this.curBasicBlock);
//
//            curSection = new Section(SectionType.QUERIES);
//            curBasicBlock = createBasicBlock();
//            this.sections.add(curSection);
//            curSection.basicBlocks.add(curBasicBlock);
//        }
//
//        curBasicBlock.queries.add(ctx.value);
//    }
//
//    // program
//
//
//    @Override
//    public void enterStatement(Template3Parser.StatementContext ctx) {
//        if(curSection.sectionType != SectionType.FUNCTION &&
//                curSection.sectionType != SectionType.NAMEDSECTION){
//            // check if named section
//            if(ctx.value.annotations.size() > 0){
//                for(AST.Annotation annotation:ctx.value.annotations){
//                    if(annotation.annotationType == AST.AnnotationType.Blk){
//                        AST.MarkerWrapper markerWrapper = (AST.MarkerWrapper) annotation.annotationValue;
//                        if(markerWrapper.marker == AST.Marker.Start){
//                            this.sectionStack.push(this.curSection);
//                            this.curSection = new Section(SectionType.NAMEDSECTION, markerWrapper.id.id);
//                            this.sections.add(this.curSection);
//                            this.curBasicBlock = createBasicBlock();
//                            this.curSection.basicBlocks.add(this.curBasicBlock);
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    @Override
//    public void exitStatement(Template3Parser.StatementContext ctx) {
//        if(ctx.value.annotations != null && ctx.value.annotations.size() > 0){
//            for(AST.Annotation annotation:ctx.value.annotations){
//                if(annotation.annotationType == AST.AnnotationType.Blk){
//                    AST.MarkerWrapper markerWrapper = (AST.MarkerWrapper) annotation.annotationValue;
//                    if(markerWrapper.marker == AST.Marker.End){
//                        this.curSection = sectionStack.pop();
//                        this.curBasicBlock = createBasicBlock();
//                    }
//                }
//            }
//        }
//    }
//
//    @Override
//    public void enterAssign(Template3Parser.AssignContext ctx) {
//        this.curBasicBlock.statements.add(ctx.value);
//    }
//
//    @Override
//    public void enterIf_stmt(Template3Parser.If_stmtContext ctx) {
//        this.curBasicBlock.statements.add(ctx.value);
//    }
//
//    @Override
//    public void enterBlock(Template3Parser.BlockContext ctx) {
//        BasicBlock basicBlock = createBasicBlock();
//    }
}

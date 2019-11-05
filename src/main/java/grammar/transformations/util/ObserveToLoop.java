package grammar.transformations.util;

import grammar.AST;
import grammar.Template3Listener;
import grammar.Template3Parser;
import grammar.cfg.BasicBlock;
import grammar.cfg.CFGBuilder;
import grammar.cfg.Section;
import grammar.cfg.Statement;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.TokenStreamRewriter;
import translators.listeners.StatementListener;
import utils.Dimension;
import utils.DimensionChecker;
import utils.Operation.Rewriter;
import utils.Utils;

import java.util.ArrayList;

public class ObserveToLoop implements Template3Listener, StatementListener {

    public TokenStreamRewriter antlrRewriter;
    public Rewriter rewriter;
    public ArrayList<Section> sections;
    public String dimMatch;

    public ObserveToLoop(CFGBuilder cfgBuilder){
        rewriter = new Rewriter(cfgBuilder.getSections());
        antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        this.sections = cfgBuilder.getSections();
    }

    @Override
    public void enterAssignmentStatement(Statement statement) {
        if(statement.statement.annotations.size() > 0){
            for(AST.Annotation annotation:statement.statement.annotations){
                if(annotation.annotationType == AST.AnnotationType.Observe) {
                    AST.AssignmentStatement samplingStatement = ((AST.AssignmentStatement) statement.statement);
                    Dimension dim;
                    dim = DimensionChecker.getDimension(samplingStatement.lhs, sections);
                    ArrayList<String> dataDim = dim.getDims();
                    dimMatch = dataDim.get(0);

                    // oneDimToLoop(statement);
                }
            }
        }
    }

    @Override
    public void enterForLoopStatement(Statement statement) {

    }

    @Override
    public void enterIfStmt(Statement statement) {

    }

    @Override
    public void enterDeclStatement(Statement statement) {

    }

    @Override
    public void enterFunctionCallStatement(Statement statement) {

    }

    @Override
    public void enterData(AST.Data data) {

    }

    void oneDimToLoop(Statement statement) {
        AST.AssignmentStatement samplingStatement = ((AST.AssignmentStatement) statement.statement);
        Dimension dim;
        dim = DimensionChecker.getDimension(samplingStatement.lhs, sections);
        ArrayList<String> dataDim = dim.getDims();
        if (dataDim.size() == 1) {
            String loop = "";
            loop = String.format("for(observe_i in 1:%s){}", dataDim.get(0));
            Template3Parser parser = Utils.readTemplateFromString(CharStreams.fromString(loop));
            AST.Program program = parser.template().value;
            BasicBlock basicBlock = new BasicBlock();
            statement.parent.getSymbolTable().fork(basicBlock);
            basicBlock.addStatement(program.statements.get(0));


            BasicBlock loopBody = new BasicBlock();
            basicBlock.getSymbolTable().fork(loopBody);
            System.out.println(statement.statement.toString());
            dimMatch = dataDim.get(0);
/*            AST.Dims newDim = new AST.Dims();
            newDim.dims.add(new AST.Id(dataDim.get(0)));
            samplingStatement.lhs = new AST.ArrayAccess((AST.Id) samplingStatement.lhs, newDim);*/

            loopBody.addStatement(statement.statement);

            basicBlock.addOutgoingEdge(loopBody, "true");
            basicBlock.addIncomingEdge(loopBody, "back");
            loopBody.addIncomingEdge(basicBlock, "true");
            loopBody.addOutgoingEdge(basicBlock, "back");

            this.rewriter.replace(statement, basicBlock.getStatements().get(0));
        }
    }


    @Override
    public void enterPrimitive(Template3Parser.PrimitiveContext ctx) {

    }

    @Override
    public void exitPrimitive(Template3Parser.PrimitiveContext ctx) {

    }

    @Override
    public void enterNumber(Template3Parser.NumberContext ctx) {

    }

    @Override
    public void exitNumber(Template3Parser.NumberContext ctx) {

    }

    @Override
    public void enterLimits(Template3Parser.LimitsContext ctx) {

    }

    @Override
    public void exitLimits(Template3Parser.LimitsContext ctx) {

    }

    @Override
    public void enterMarker(Template3Parser.MarkerContext ctx) {

    }

    @Override
    public void exitMarker(Template3Parser.MarkerContext ctx) {

    }

    @Override
    public void enterAnnotation_type(Template3Parser.Annotation_typeContext ctx) {

    }

    @Override
    public void exitAnnotation_type(Template3Parser.Annotation_typeContext ctx) {

    }

    @Override
    public void enterAnnotation_value(Template3Parser.Annotation_valueContext ctx) {

    }

    @Override
    public void exitAnnotation_value(Template3Parser.Annotation_valueContext ctx) {

    }

    @Override
    public void enterAnnotation(Template3Parser.AnnotationContext ctx) {

    }

    @Override
    public void exitAnnotation(Template3Parser.AnnotationContext ctx) {

    }

    @Override
    public void enterDims(Template3Parser.DimsContext ctx) {

    }

    @Override
    public void exitDims(Template3Parser.DimsContext ctx) {

    }

    @Override
    public void enterDim(Template3Parser.DimContext ctx) {

    }

    @Override
    public void exitDim(Template3Parser.DimContext ctx) {

    }

    @Override
    public void enterDtype(Template3Parser.DtypeContext ctx) {

    }

    @Override
    public void exitDtype(Template3Parser.DtypeContext ctx) {

    }

    @Override
    public void enterArray(Template3Parser.ArrayContext ctx) {

    }

    @Override
    public void exitArray(Template3Parser.ArrayContext ctx) {

    }

    @Override
    public void enterVector(Template3Parser.VectorContext ctx) {

    }

    @Override
    public void exitVector(Template3Parser.VectorContext ctx) {

    }

    @Override
    public void enterData(Template3Parser.DataContext ctx) {

    }

    @Override
    public void exitData(Template3Parser.DataContext ctx) {

    }

    @Override
    public void enterFunction_call(Template3Parser.Function_callContext ctx) {

    }

    @Override
    public void exitFunction_call(Template3Parser.Function_callContext ctx) {

    }

    @Override
    public void enterFor_loop(Template3Parser.For_loopContext ctx) {

    }

    @Override
    public void exitFor_loop(Template3Parser.For_loopContext ctx) {

    }

    @Override
    public void enterIf_stmt(Template3Parser.If_stmtContext ctx) {

    }

    @Override
    public void exitIf_stmt(Template3Parser.If_stmtContext ctx) {

    }

    @Override
    public void enterAssign(Template3Parser.AssignContext ctx) {

    }

    @Override
    public void exitAssign(Template3Parser.AssignContext ctx) {

    }

    @Override
    public void enterDecl(Template3Parser.DeclContext ctx) {

    }

    @Override
    public void exitDecl(Template3Parser.DeclContext ctx) {

    }

    @Override
    public void enterStatement(Template3Parser.StatementContext ctx) {
    }

    @Override
    public void exitStatement(Template3Parser.StatementContext ctx) {
        for (AST.Annotation annotation: ctx.value.annotations){
            if(annotation.annotationType == AST.AnnotationType.Observe){
//                for (int childID=0; 0 < ctx.getChildCount(); childID++) {
//                    if (ctx.getChild(childID) instanceof Template3Parser.ExprContext) {
//                        Template3Parser.ExprContext childCtx = (Template3Parser.ExprContext) ctx.getChild(childID);
//                    }
//                }
                antlrRewriter.insertBefore(ctx.getStart(), String.format("for(observe_i in 1:%1$s){\n", dimMatch));
                antlrRewriter.insertAfter(ctx.getStop(), String.format("\n}"));
            }
        }


    }

    @Override
    public void enterBlock(Template3Parser.BlockContext ctx) {

    }

    @Override
    public void exitBlock(Template3Parser.BlockContext ctx) {

    }
    @Override
    public void enterExpr(Template3Parser.ExprContext ctx) {
        if (ctx.ID() != null) {
            ArrayList<String> idDims = DimensionChecker.getDimension(ctx.value, sections).getDims();
            if (idDims.size() > 0 && idDims.get(0).equals(dimMatch)) {
                // AST.Dims newDim = new AST.Dims();
                // newDim.dims.add(new AST.Id(dimMatch));
                System.out.println(ctx.getStart());
                System.out.println(ctx.getStop());
                antlrRewriter.replace(ctx.getStart(), ctx.getStop(), ctx.ID().toString() + "[observe_i]");
                //ctx.value = new AST.ArrayAccess(new AST.Id(ctx.ID().toString()), newDim);
                System.out.println(ctx.getText());
            }
        }

    }

    @Override
    public void exitExpr(Template3Parser.ExprContext ctx) {

    }

    @Override
    public void enterQuery(Template3Parser.QueryContext ctx) {

    }

    @Override
    public void exitQuery(Template3Parser.QueryContext ctx) {

    }

    @Override
    public void enterTemplate(Template3Parser.TemplateContext ctx) {

    }

    @Override
    public void exitTemplate(Template3Parser.TemplateContext ctx) {

    }

    @Override
    public void visitTerminal(TerminalNode terminalNode) {

    }

    @Override
    public void visitErrorNode(ErrorNode errorNode) {

    }

    @Override
    public void enterEveryRule(ParserRuleContext parserRuleContext) {

    }

    @Override
    public void exitEveryRule(ParserRuleContext parserRuleContext) {

    }

    // public ArrayList<AST.Id> getSameDimIds(AST.Expression rhs, Dimension dim) {
    //     String dimMatch = dim.getDims().get(0);
    //     for (ss: rhs)

    //     DimensionChecker(statement,)

    // }
}

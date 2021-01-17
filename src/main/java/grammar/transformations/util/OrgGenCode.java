package grammar.transformations.util;

import grammar.AST;
import grammar.StanParser;
import grammar.Template3Listener;
import grammar.Template3Parser;
import grammar.cfg.CFGBuilder;
import grammar.cfg.Section;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

public class OrgGenCode implements Template3Listener {
    // Run after ObserveToLoop and SampleToTarget
    public ArrayList<Section> sections;
    private String dimMatch;
    private String iMatch;
    private Boolean inFor_loop=false;
    private String dataName;
    public String genCode;
    public String nplCode;
    private String samplingStr;

    public OrgGenCode(CFGBuilder cfgBuilder) {
        this.sections = cfgBuilder.getSections();
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
        this.dimMatch = ctx.e2.getText();
        this.iMatch = ctx.value.loopVar.id;
        this.inFor_loop = true;

    }

    @Override
    public void exitFor_loop(Template3Parser.For_loopContext ctx) {
        this.inFor_loop = false;

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
        for (AST.Annotation annotation: ctx.value.annotations) {
            if (annotation.annotationType == AST.AnnotationType.Observe) {
                AST.AssignmentStatement samplingStatement = ((AST.AssignmentStatement) ctx.value);
                AST.FunctionCall functionCall = (AST.FunctionCall) samplingStatement.rhs;
                String distName = functionCall.id.id;
                samplingStr = ctx.value.toString().replace(distName, distName + "_rng");
                dataName = samplingStatement.lhs.toString().split("\\[")[0];
            }
        }

    }

    @Override
    public void exitStatement(Template3Parser.StatementContext ctx) {
    }

    @Override
    public void enterBlock(Template3Parser.BlockContext ctx) {

    }

    @Override
    public void exitBlock(Template3Parser.BlockContext ctx) {

    }

    @Override
    public void enterExpr(Template3Parser.ExprContext ctx) {

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
        genCode =
                "\n@blk start generatedquantities\nfloat "+ dataName+"_test[" + dimMatch +"]\n"
                       // + transParamCode
                        + "@blk end generatedquantities\n"
                        + "for (" + iMatch + " in 1:" + dimMatch + ") {\n"
                        + samplingStr.replaceAll("\\b" + dataName + "\\b", dataName + "_test")
                        + "\n}\n";

        nplCode = dataName + "_corrupted\n";

        // Change Sampling to lpdf
        String newLikFunc = samplingStr.replace("rng","lpdf");
        newLikFunc = newLikFunc.split("=")[1];
        newLikFunc = newLikFunc.replaceFirst("\\(", "(z_test[observe_i] | ");
        // get New Gen Code
        String newGenCode = "generated quantities{\n" +
                "real lik;\n" +
                "lik = 0;\n" +
                "for (" + iMatch + " in 1:" + dimMatch + ") {\n" +
                "\t   lik = lik+ " + newLikFunc + ";\n" +
                "}";
        System.out.println(newGenCode);
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

}

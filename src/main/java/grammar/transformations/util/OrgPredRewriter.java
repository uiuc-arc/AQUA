package grammar.transformations.util;


import grammar.Template3Listener;
import grammar.Template3Parser;
import grammar.cfg.CFGBuilder;
import grammar.cfg.Section;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;

public class OrgPredRewriter implements Template3Listener {
    public TokenStreamRewriter antlrRewriter;
    public ArrayList<Section> sections;
    public String dimMatch;
    public String iMatch;
    public Token pl_orgDeclStop;
    public Token ps_org_corrDeclStop;
    public Token ps_org_corrInitStop;
    public Token ps_org_corrAssignStop;
    public Token pl_orgAssignStop;
    public Token lastForStop;
    public String dataCorrupted;
    private Boolean inFor_loop = false;

    public OrgPredRewriter(CFGBuilder cfgBuilder, TokenStreamRewriter antlrRewriter) {
        System.out.println("========In OrgPredRewriter========");
        this.antlrRewriter = antlrRewriter;
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
        if (ctx.getText().contains("_corrupted"))
            dataCorrupted = ctx.decl.ID.getText();

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
        this.lastForStop = ctx.block.getStart();

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
        if (ctx.getText().contains("pl_org=")) {
            pl_orgAssignStop = ctx.getStop();
        } else if (ctx.getText().contains("ps_org_corr=0")) {
            ps_org_corrInitStop = ctx.getStop();
        } else if (ctx.getText().contains("ps_org_corr=ps_org_corr")) {
            ps_org_corrAssignStop = ctx.getStop();
        }

    }

    @Override
    public void exitAssign(Template3Parser.AssignContext ctx) {

    }

    @Override
    public void enterDecl(Template3Parser.DeclContext ctx) {
        if (ctx.getText().contains("floatpl_org")) {
            pl_orgDeclStop = ctx.getStop();
        } else if (ctx.getText().contains("floatps_org_corr")) {
            ps_org_corrDeclStop = ctx.getStop();
        }

    }

    @Override
    public void exitDecl(Template3Parser.DeclContext ctx) {

    }

    @Override
    public void enterStatement(Template3Parser.StatementContext ctx) {

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

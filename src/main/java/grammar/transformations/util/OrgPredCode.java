package grammar.transformations.util;

import grammar.AST;
import grammar.Template3Listener;
import grammar.Template3Parser;
import grammar.cfg.CFGBuilder;
import grammar.cfg.Section;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;

import java.util.ArrayList;

public class OrgPredCode implements Template3Listener {
    // Run after ObserveToLoop and SampleToTarget
    public TokenStreamRewriter antlrRewriter;
    public ArrayList<Section> sections;
    private Token lastDataStop;
    private String dimMatch;
    private String iMatch;
    private ArrayList<Template3Parser.DataContext> dataList = new ArrayList<Template3Parser.DataContext>();
    private Boolean inFor_loop=false;
    private String dataCorrupted;
    private Boolean yAdded=false;
    private String psGood = "";
    private String psCorr = "";

    public OrgPredCode(CFGBuilder cfgBuilder, TokenStreamRewriter antlrRewriter) {
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
            dataList.add(ctx);

    }

    @Override
    public void exitData(Template3Parser.DataContext ctx) {
        this.lastDataStop = ctx.getStop();

    }

    @Override
    public void enterFunction_call(Template3Parser.Function_callContext ctx) {
        if (inFor_loop && ! yAdded) {
            ArrayList<AST.Expression> params = ctx.value.parameters;
            if (params.size() > 1) {
                String currParam = params.get(0).toString().split("\\[")[0];
                for (Template3Parser.DataContext dd : dataList) {
                    if (dd.value.decl.id.toString().equals(currParam)) {
                        dataCorrupted = currParam;
                        // if (dd.value.array != null && dd.value.decl != null)
                        String dataDim = "";
                        if (dd.value.decl.dims != null) {
                            dataDim = "[" + dd.value.decl.dims.toString() + "]";
                            dimMatch = dd.value.decl.dims.toString();
                        } else {
                            dimMatch = dd.value.decl.dtype.dims.toString();
                        }
                        String dataDecl = "\n" + dd.value.decl.dtype.toString().replace("FLOAT", "float").
                                replace("INTEGER", "int").
                                replace("VECTOR", "vector").replace("MATRIX", "matrix")
                                    + " " + dd.value.decl.id.toString()
                                    + "_corrupted" + dataDim
                                    + " : " + dd.array.getText() + "\n";
                        antlrRewriter.insertAfter(lastDataStop,dataDecl);
                        iMatch = params.get(0).toString().split("\\[")[1].split("\\]")[0];
                        yAdded = true;
                    }
                }
            }
        }

    }

    @Override
    public void exitFunction_call(Template3Parser.Function_callContext ctx) {

    }

    @Override
    public void enterFor_loop(Template3Parser.For_loopContext ctx) {
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
        if (ctx.e1.getText().equals("target")) {
            if (! psGood.equals("")) {
                psGood += " + ";
                psCorr += " + ";
            }
            psGood += ctx.e2.getChild(2).getText();
            psCorr += ctx.e2.getChild(2).getText().replaceAll("\\b" + dataCorrupted + "\\b" , dataCorrupted + "_corrupted");
        }

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
        antlrRewriter.insertAfter(ctx.getStop(),
                "\n@blk start generatedquantities\nfloat pl_org\n"
                        + "@blk end generatedquantities\n"
                        + "if(1)\n{\nfloat ps_org_good\n"
                        + "float ps_org_corr\n"
                        + "ps_org_good=0\n"
                        + "ps_org_corr=0\n"
                        + "for (" + iMatch + " in 1:" + dimMatch + ") {\n"
                        + String.format("ps_org_good=ps_org_good+%s\n", psGood)
                        + String.format("ps_org_corr=ps_org_corr+%s\n", psCorr)
                        + "}\n"
                        + "pl_org = exp(ps_org_corr)\n"
                        + "}\n");

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

package grammar.transformations;

import grammar.AST;
import grammar.Template3Listener;
import grammar.Template3Parser;
import grammar.cfg.CFGBuilder;
import grammar.cfg.Section;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import utils.Utils;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

public class ReparamLocalizer implements Template3Listener {
    private final List<JsonObject> models= Utils.getDistributions(null);
    public TokenStreamRewriter antlrRewriter;
    public ArrayList<Section> sections;
    public Boolean transformed=false;
    private Token lastDeclStop;
    private ArrayList<String> dataList = new ArrayList<>();
    private String dimMatch;
    private String iMatch;
    private Boolean inFor_loop = false;
    private String priorAddedForParam = "";
    private Token startLastAssign;

    public ReparamLocalizer(CFGBuilder cfgBuilder, TokenStreamRewriter antlrRewriter) {
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
        dataList.add(ctx.decl.ID.getText());

    }

    @Override
    public void exitData(Template3Parser.DataContext ctx) {

    }

    @Override
    public void enterFunction_call(Template3Parser.Function_callContext ctx) {
        if (inFor_loop && ctx.ID.getText().contains("normal")) {
            ArrayList<AST.Expression> params = ctx.value.parameters;
            if (params.size() > 0) {
                if (dataList.contains(params.get(0).toString().split("\\[")[0])) {
                    ParserRuleContext paramToTransformCtx = ctx.expr(2);
                    // Add robust_local_param prior normal(original_param, 0.25)
                    // replace original_param
                    String newParamName = paramToTransformCtx.getText().replaceAll("[^a-zA-Z0-9_]", "");
                    antlrRewriter.replace(paramToTransformCtx.getStart(), paramToTransformCtx.getStop(), String.format("inv_sqrt(robust_local_tau%3$s[%1$s])*(%2$s)", iMatch, paramToTransformCtx.getText(),newParamName));
                    if (!priorAddedForParam.equals(newParamName)) {
                        // robust_local_tau ~ gamma(robust_local_nu/2, robust_local_nu/2);
                        antlrRewriter.insertBefore(startLastAssign, String.format("robust_local_tau%2$s[%1$s] = gamma(robust_local_nu%2$s/2, robust_local_nu%2$s/2)\n", iMatch, newParamName));
                        // Add decl for robust_local_param
                        antlrRewriter.insertAfter(lastDeclStop,
                                String.format("\n@prior\n@limits %3$s\nfloat %1$s[%2$s]\n", "robust_local_tau"+newParamName, dimMatch, "<lower=0,upper=10>"));
                        antlrRewriter.insertAfter(lastDeclStop,
                                String.format("\n@prior\n@limits %2$s\nfloat %1$s\n", "robust_local_nu"+newParamName, "<lower=0,upper=10>"));
                        priorAddedForParam=(newParamName);
                    }

                    transformed = true;

                }

            }

        }

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
        startLastAssign = ctx.getStart();

    }

    @Override
    public void exitAssign(Template3Parser.AssignContext ctx) {

    }

    @Override
    public void enterDecl(Template3Parser.DeclContext ctx) {

    }

    @Override
    public void exitDecl(Template3Parser.DeclContext ctx) {
        for (AST.Annotation annotation: ctx.value.annotations) {
            if (annotation.annotationType == AST.AnnotationType.Prior) {
                lastDeclStop = ctx.getStop();
            }
        }

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

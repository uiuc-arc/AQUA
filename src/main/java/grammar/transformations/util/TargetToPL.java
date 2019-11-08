package grammar.transformations.util;

import grammar.AST;
import grammar.Template3Listener;
import grammar.Template3Parser;
import grammar.cfg.Section;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import utils.Utils;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class TargetToPL implements Template3Listener {
    private final List<JsonObject> models= Utils.getDistributions(null);
    public String dimMatch;
    public String iMatch;
    private Boolean inFor_loop = false;
    private OrgPredRewriter orgPredRewriter;
    private String psGood = "";
    private String psCorr = "";
    private String transName;

    public TargetToPL(OrgPredRewriter orgPredRewriter, String transName) {
        System.out.println("========In TargetToPL " + transName + "========");
        this.orgPredRewriter = orgPredRewriter;
        this.transName = transName;
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
        String lhs = ctx.e1.getText();
        if (lhs.contains("robust_local") ||
                lhs.contains("robust_const")) {
            String dist;
            dist = ctx.e2.getText().split("\\(")[0];
            String newDist = dist+"_rng";
            String newSampling = lhs.split("\\[")[0]+"="+ctx.e2.getText().replace(dist,newDist);
            System.out.println(newSampling);
            orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.lastForStop, "\n" + newSampling);

        }


    }

    @Override
    public void exitAssign(Template3Parser.AssignContext ctx) {
        if (ctx.e1.getText().equals("target")) {
            if (! psGood.equals("")) {
                psGood += " + ";
                psCorr += " + ";
            }
            psGood += ctx.e2.getChild(2).getText();
            psCorr += ctx.e2.getChild(2).getText().replaceAll(
                    orgPredRewriter.dataCorrupted.replace("_corrupted","") + "\\[" + orgPredRewriter.iMatch + "\\]",
                    orgPredRewriter.dataCorrupted + "\\[" + orgPredRewriter.iMatch + "\\]");
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
        Boolean isPrior = false;
        for (AST.Annotation annotation: ctx.value.annotations) {
            if (annotation.annotationType == AST.AnnotationType.Prior) {
                isPrior = true;

            } else if (annotation.annotationType == AST.AnnotationType.Limits)
                System.out.println(annotation.annotationValue.toString());
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
        // pl * 2
        orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.pl_orgDeclStop,
                "\nfloat pl_" + transName + "\nfloat pl_" + transName + "_c\n" );
        // ps decl *2
        orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.ps_org_corrDeclStop,
                "\nfloat ps_" + transName + "_good\nfloat ps_" + transName + "_corr\n" );
        // ps init *2
        orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.ps_org_corrInitStop,
                "\nps_" + transName + "_good=0\nps_" + transName + "_corr=0\n" );
        // ps assign *2
        orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.ps_org_corrAssignStop,
                String.format("\n%1$s=%1$s+%2$s\n%3$s=%3$s+%4$s\n",
                        "ps_" + transName + "_good", psGood , "ps_" + transName + "_corr", psCorr));
        // pl assign * 2
        orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.pl_orgAssignStop,
                String.format("\npl_%1$s=pl_RU(ps_%1$s_corr, ps_%1$s_good, ps_org_good)", transName) +
                        String.format("\npl_%1$s_c= pl_RL(ps_%1$s_good, ps_org_good)\n",transName));

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

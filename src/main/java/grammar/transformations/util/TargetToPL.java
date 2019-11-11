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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class TargetToPL implements Template3Listener {
    private final List<JsonObject> models= Utils.getDistributions(null);
    public String dimMatch;
    public String iMatch;
    private Boolean inFor_loop = false;
    private OrgPredRewriter orgPredRewriter;
    private String psGood = "";
    private String psCorr = "";
    private String transName;
    private HashMap<String,String> newParamLimits = new HashMap<>();

    public TargetToPL(OrgPredRewriter orgPredRewriter, String transName) {
        // System.out.println("========In TargetToPL " + transName + "========");
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
        String lhs = ctx.e1.getText().split("\\[")[0];
        if (lhs.contains("robust_")) {
            String dist;
            dist = ctx.e2.getText().split("\\(")[0];
            String[] paramList = ctx.e2.getText().split("[^\\w']+");
            String newDist = dist+"_rng";
            String newSampling = ctx.e2.getText().replace(dist,newDist);
            String anotherParam = null;
            for (String ss:paramList) {
                if (ss.startsWith("robust_") && !lhs.equals(ss)) {
                    anotherParam = ss;
                    break;
                }
            }
            if (anotherParam != null) {
                String anotherLimit = newParamLimits.get(anotherParam);
                if (anotherLimit != null) {
                    String lowerLimit = anotherLimit.split("[=,>]")[1];
                    String upperLimit = anotherLimit.split("[=,>]")[3];
                    orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.lastForStop, "\nfloat " + anotherParam);
                    orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.ps_org_corrAssignStop,
                            "\n" + anotherParam + String.format("=uniform_rng(%1$s,%2$s)", lowerLimit, upperLimit));
                    newParamLimits.remove(anotherParam);
                }
            }
            String limit = newParamLimits.get(lhs);
            if (limit != null) {
                String[] limits = limit.split("([<>, ])");
                for (String ll:limits) {
                    if (ll.contains("lower=")) {
                        String lowerBound = ll.split("=")[1];
                        if (ll.split("=")[1].equals("0"))
                            lowerBound = "0.0000000000000001";
                        newSampling = "fmax(" + newSampling + "," + lowerBound + ")";
                    } else if (ll.contains("upper=")) {
                        newSampling = "fmin(" + newSampling + "," + ll.split("=")[1] + ")";
                    }
                }
            }
            orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.lastForStop, "\nfloat " + lhs);
            orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.ps_org_corrAssignStop, "\n" + lhs + "=" + newSampling);
            newParamLimits.remove(lhs);
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
            psCorr += ctx.e2.getChild(2).getText();
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
        String limits = null;
        for (AST.Annotation annotation: ctx.value.annotations) {
            if (annotation.annotationType == AST.AnnotationType.Prior) {
                isPrior = true;

            } else if (annotation.annotationType == AST.AnnotationType.Limits)
                limits = annotation.annotationValue.toString();
        }
        if (isPrior && limits != null && ctx.decl.ID.getText().contains("robust_"))
            newParamLimits.put(ctx.decl.ID.getText(), limits);
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
                "\nfloat pl_" + transName + "\nfloat pl_" + transName + "_c" );
        // ps decl *2
        orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.ps_org_corrDeclStop,
                "\nfloat ps_" + transName + "_good\nfloat ps_" + transName + "_corr" );
        // ps init *2
        orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.ps_org_corrInitStop,
                "\nps_" + transName + "_good=0\nps_" + transName + "_corr=0" );
        // ps assign *2
        String psGoodCopy = psGood;
        for (String pp: psGoodCopy.split("[^\\w']+")) {
            if (pp.startsWith("robust_")) {
                psGood = psGood.replaceAll(pp + "\\[" + iMatch + "\\]", pp);
                psCorr = psCorr.replaceAll(pp + "\\[" + iMatch + "\\]", pp);
            }
        }
        //TODO: match only dataCorrupted. Now it can also match sigma_y[iMatch]
        psCorr = psCorr.replaceAll("\\b" +
                orgPredRewriter.dataCorrupted.replace("_corrupted","")+ "\\b", // + "\\[" + orgPredRewriter.iMatch + "\\]",
                orgPredRewriter.dataCorrupted); // + "\\[" + orgPredRewriter.iMatch + "\\]");
        if (transName.equals("robust_reweight")) {
                psCorr += orgPredRewriter.reweighterCorrection;
        }
        orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.ps_org_corrAssignStop,
                String.format("\n%1$s=%1$s+%2$s\n%3$s=%3$s+%4$s",
                        "ps_" + transName + "_good", psGood , "ps_" + transName + "_corr", psCorr));
        // pl assign * 2
        orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.pl_orgAssignStop,
                String.format("\npl_%1$s=pl_RU(ps_%1$s_corr, ps_%1$s_good, ps_org_good)", transName) +
                        String.format("\npl_%1$s_c=pl_RL(ps_%1$s_good, ps_org_good)",transName));

        ArrayList<String> keySet = new ArrayList<String>(newParamLimits.keySet());
        for (String anotherParam: keySet) {
            String anotherLimit = newParamLimits.get(anotherParam);
            if (anotherLimit != null) {
                String lowerLimit = anotherLimit.split("[=,>]")[1];
                String upperLimit = anotherLimit.split("[=,>]")[3];
                orgPredRewriter.antlrRewriter.insertAfter(orgPredRewriter.lastForStop, "\nfloat " + anotherParam);
                orgPredRewriter.antlrRewriter.insertBefore(orgPredRewriter.ps_org_goodAssignStart,
                         anotherParam + String.format("=uniform_rng(%1$s,%2$s)\n", lowerLimit, upperLimit));
                newParamLimits.remove(anotherParam);
            }
        }

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

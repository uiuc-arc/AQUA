package translators;

import grammar.Template3Listener;
import grammar.Template3Parser;
import grammar.cfg.BasicBlock;
import grammar.cfg.CFGBuilder;
import grammar.cfg.Section;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;

public class CPPTranslator implements Template3Listener {
    private OutputStream out;
    public ArrayList<Section> sections;
    private ArrayList<String> dataList = new ArrayList<>();

    public CPPTranslator(CFGBuilder cfgBuilder, OutputStream o){
        this.sections = cfgBuilder.getSections();
        out = o;
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
        System.out.println(dataList);

    }

    @Override
    public void exitData(Template3Parser.DataContext ctx) {

    }

    @Override
    public void enterFunction_call(Template3Parser.Function_callContext ctx) {
        if (ctx.value.parameters.size() > 1 && !ctx.ID.getText().contains("cov_exp_quad")) {
            if (dataList.contains(ctx.value.parameters.get(0).toString())) {
                System.out.println(ctx.ID.getText());
            }
        }

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
    public void dump(String str) {
        try {
            out.write(str.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

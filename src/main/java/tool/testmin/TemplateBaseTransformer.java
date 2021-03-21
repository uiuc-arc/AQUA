package tool.testmin;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import grammar.Template2Listener;
import grammar.Template2Parser;
import tool.testmin.util.MyLogger;

public class TemplateBaseTransformer implements Template2Listener {

    protected Template2Parser parser;
    protected TokenStreamRewriter rewriter;
    protected MyLogger logger;

    public TemplateBaseTransformer(Template2Parser parser, MyLogger logger) {
        this.parser = parser;
        this.rewriter = new TokenStreamRewriter(this.parser.getTokenStream());
        this.logger = logger;
    }

    @Override
    public void enterSubset(Template2Parser.SubsetContext ctx) {

    }


    @Override
    public void exitSubset(Template2Parser.SubsetContext ctx) {

    }

    @Override
    public void enterVecdivop(Template2Parser.VecdivopContext ctx) {

    }

    @Override
    public void exitVecdivop(Template2Parser.VecdivopContext ctx) {


    }

    @Override
    public void enterVecmulop(Template2Parser.VecmulopContext ctx) {

    }

    @Override
    public void exitVecmulop(Template2Parser.VecmulopContext ctx) {

    }

    @Override
    public void enterPrimitive(Template2Parser.PrimitiveContext ctx) {

    }

    @Override
    public void exitPrimitive(Template2Parser.PrimitiveContext ctx) {

    }

    @Override
    public void enterNumber(Template2Parser.NumberContext ctx) {

    }

    @Override
    public void exitNumber(Template2Parser.NumberContext ctx) {

    }

    @Override
    public void enterDtype(Template2Parser.DtypeContext ctx) {

    }

    @Override
    public void exitDtype(Template2Parser.DtypeContext ctx) {

    }

    @Override
    public void enterArray(Template2Parser.ArrayContext ctx) {

    }

    @Override
    public void exitArray(Template2Parser.ArrayContext ctx) {

    }

    @Override
    public void enterVector(Template2Parser.VectorContext ctx) {

    }

    @Override
    public void exitVector(Template2Parser.VectorContext ctx) {

    }

    @Override
    public void enterDims(Template2Parser.DimsContext ctx) {

    }

    @Override
    public void exitDims(Template2Parser.DimsContext ctx) {

    }

    @Override
    public void enterVectorDIMS(Template2Parser.VectorDIMSContext ctx) {

    }

    @Override
    public void exitVectorDIMS(Template2Parser.VectorDIMSContext ctx) {

    }

    @Override
    public void enterDecl(Template2Parser.DeclContext ctx) {

    }

    @Override
    public void exitDecl(Template2Parser.DeclContext ctx) {

    }

    @Override
    public void enterLimits(Template2Parser.LimitsContext ctx) {

    }

    @Override
    public void exitLimits(Template2Parser.LimitsContext ctx) {

    }

    @Override
    public void enterData(Template2Parser.DataContext ctx) {

    }

    @Override
    public void exitData(Template2Parser.DataContext ctx) {

    }

    @Override
    public void enterPrior(Template2Parser.PriorContext ctx) {

    }

    @Override
    public void exitPrior(Template2Parser.PriorContext ctx) {

    }

    @Override
    public void enterParam(Template2Parser.ParamContext ctx) {

    }

    @Override
    public void exitParam(Template2Parser.ParamContext ctx) {

    }

    @Override
    public void enterParams(Template2Parser.ParamsContext ctx) {

    }

    @Override
    public void exitParams(Template2Parser.ParamsContext ctx) {

    }

    @Override
    public void enterDistexpr(Template2Parser.DistexprContext ctx) {

    }

    @Override
    public void exitDistexpr(Template2Parser.DistexprContext ctx) {

    }

    @Override
    public void enterLoopcomp(Template2Parser.LoopcompContext ctx) {

    }

    @Override
    public void exitLoopcomp(Template2Parser.LoopcompContext ctx) {

    }

    @Override
    public void enterFor_loop(Template2Parser.For_loopContext ctx) {

    }

    @Override
    public void exitFor_loop(Template2Parser.For_loopContext ctx) {

    }

    @Override
    public void enterIf_stmt(Template2Parser.If_stmtContext ctx) {

    }

    @Override
    public void exitIf_stmt(Template2Parser.If_stmtContext ctx) {

    }

    @Override
    public void enterElse_blk(Template2Parser.Else_blkContext ctx) {

    }

    @Override
    public void exitElse_blk(Template2Parser.Else_blkContext ctx) {

    }

    @Override
    public void enterFunction_call(Template2Parser.Function_callContext ctx) {

    }

    @Override
    public void exitFunction_call(Template2Parser.Function_callContext ctx) {

    }

    @Override
    public void enterFparam(Template2Parser.FparamContext ctx) {

    }

    @Override
    public void exitFparam(Template2Parser.FparamContext ctx) {

    }

    @Override
    public void enterFparams(Template2Parser.FparamsContext ctx) {

    }

    @Override
    public void exitFparams(Template2Parser.FparamsContext ctx) {

    }

    @Override
    public void enterReturn_or_param_type(Template2Parser.Return_or_param_typeContext ctx) {

    }

    @Override
    public void exitReturn_or_param_type(Template2Parser.Return_or_param_typeContext ctx) {

    }

    @Override
    public void enterFunction_decl(Template2Parser.Function_declContext ctx) {

    }

    @Override
    public void exitFunction_decl(Template2Parser.Function_declContext ctx) {

    }

    @Override
    public void enterBlock(Template2Parser.BlockContext ctx) {

    }

    @Override
    public void exitBlock(Template2Parser.BlockContext ctx) {

    }

    @Override
    public void enterTransformedparam(Template2Parser.TransformedparamContext ctx) {

    }

    @Override
    public void exitTransformedparam(Template2Parser.TransformedparamContext ctx) {

    }

    @Override
    public void enterTransformeddata(Template2Parser.TransformeddataContext ctx) {

    }

    @Override
    public void exitTransformeddata(Template2Parser.TransformeddataContext ctx) {

    }

    @Override
    public void enterGeneratedquantities(Template2Parser.GeneratedquantitiesContext ctx) {

    }

    @Override
    public void exitGeneratedquantities(Template2Parser.GeneratedquantitiesContext ctx) {

    }

    @Override
    public void enterFunctions(Template2Parser.FunctionsContext ctx) {

    }

    @Override
    public void exitFunctions(Template2Parser.FunctionsContext ctx) {

    }

    @Override
    public void enterVal(Template2Parser.ValContext ctx) {

    }

    @Override
    public void exitVal(Template2Parser.ValContext ctx) {

    }

    @Override
    public void enterDivop(Template2Parser.DivopContext ctx) {

    }

    @Override
    public void exitDivop(Template2Parser.DivopContext ctx) {

    }

    @Override
    public void enterString(Template2Parser.StringContext ctx) {

    }

    @Override
    public void exitString(Template2Parser.StringContext ctx) {

    }

    @Override
    public void enterExponop(Template2Parser.ExponopContext ctx) {

    }

    @Override
    public void exitExponop(Template2Parser.ExponopContext ctx) {

    }

    @Override
    public void enterArray_access(Template2Parser.Array_accessContext ctx) {

    }

    @Override
    public void exitArray_access(Template2Parser.Array_accessContext ctx) {

    }

    @Override
    public void enterAddop(Template2Parser.AddopContext ctx) {

    }

    @Override
    public void exitAddop(Template2Parser.AddopContext ctx) {

    }

    @Override
    public void enterMinusop(Template2Parser.MinusopContext ctx) {

    }

    @Override
    public void exitMinusop(Template2Parser.MinusopContext ctx) {

    }

    @Override
    public void enterLt(Template2Parser.LtContext ctx) {

    }

    @Override
    public void exitLt(Template2Parser.LtContext ctx) {

    }

    @Override
    public void enterUnary(Template2Parser.UnaryContext ctx) {

    }

    @Override
    public void exitUnary(Template2Parser.UnaryContext ctx) {

    }

    @Override
    public void enterEq(Template2Parser.EqContext ctx) {

    }

    @Override
    public void exitEq(Template2Parser.EqContext ctx) {

    }

    @Override
    public void enterGt(Template2Parser.GtContext ctx) {

    }

    @Override
    public void exitGt(Template2Parser.GtContext ctx) {

    }

    @Override
    public void enterBrackets(Template2Parser.BracketsContext ctx) {

    }

    @Override
    public void exitBrackets(Template2Parser.BracketsContext ctx) {

    }

    @Override
    public void enterRef(Template2Parser.RefContext ctx) {

    }

    @Override
    public void exitRef(Template2Parser.RefContext ctx) {

    }

    @Override
    public void enterGeq(Template2Parser.GeqContext ctx) {

    }

    @Override
    public void exitGeq(Template2Parser.GeqContext ctx) {

    }

    @Override
    public void enterMulop(Template2Parser.MulopContext ctx) {

    }

    @Override
    public void exitMulop(Template2Parser.MulopContext ctx) {

    }

    @Override
    public void enterAnd(Template2Parser.AndContext ctx) {

    }

    @Override
    public void exitAnd(Template2Parser.AndContext ctx) {

    }

    @Override
    public void enterFunction(Template2Parser.FunctionContext ctx) {

    }

    @Override
    public void exitFunction(Template2Parser.FunctionContext ctx) {

    }

    @Override
    public void enterNe(Template2Parser.NeContext ctx) {

    }

    @Override
    public void exitNe(Template2Parser.NeContext ctx) {

    }

    @Override
    public void enterLeq(Template2Parser.LeqContext ctx) {

    }

    @Override
    public void exitLeq(Template2Parser.LeqContext ctx) {

    }

    @Override
    public void enterTranspose(Template2Parser.TransposeContext ctx) {

    }

    @Override
    public void exitTranspose(Template2Parser.TransposeContext ctx) {

    }

    @Override
    public void enterTernary(Template2Parser.TernaryContext ctx) {

    }

    @Override
    public void exitTernary(Template2Parser.TernaryContext ctx) {

    }

    @Override
    public void enterAssign(Template2Parser.AssignContext ctx) {

    }

    @Override
    public void exitAssign(Template2Parser.AssignContext ctx) {

    }

    @Override
    public void enterObserve(Template2Parser.ObserveContext ctx) {

    }

    @Override
    public void exitObserve(Template2Parser.ObserveContext ctx) {

    }

    @Override
    public void enterStatement(Template2Parser.StatementContext ctx) {

    }

    @Override
    public void exitStatement(Template2Parser.StatementContext ctx) {

    }

    @Override
    public void enterQuery(Template2Parser.QueryContext ctx) {

    }

    @Override
    public void exitQuery(Template2Parser.QueryContext ctx) {

    }

    @Override
    public void enterTemplate(Template2Parser.TemplateContext ctx) {

    }

    @Override
    public void exitTemplate(Template2Parser.TemplateContext ctx) {

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

    protected void print(String message) {
        if (this.logger != null) {
            logger.print(message, true);
        } else {
            String threadName = Thread.currentThread().getName();
            MyLogger logger = MyLogger.LoggerPool.get(threadName);
            if (logger != null) {
                logger.print(message, true);
            } else {
                System.out.println(message);
            }
        }
    }
}
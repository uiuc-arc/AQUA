package tool.testmin.util;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import grammar.StanListener;
import grammar.StanParser;

public class PartsCounter implements StanListener {

    protected StanParser parser;
    private int partsCount;
    private int datatypes;
    private int dataStructures;

    public PartsCounter(String testfile) {
        this(TMUtil.getParser(testfile));
    }

    public PartsCounter(StanParser parser) {
        this.parser = parser;
        partsCount = 0;
        datatypes = 0;
        dataStructures = 0;
    }

    public static int Count(String testfile) {
        PartsCounter partsCounter = new PartsCounter(testfile);
        ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
        parseTreeWalker.walk(partsCounter, partsCounter.parser.program());
        return partsCounter.getCount();
    }

    public static int CountFromString(String content) {
        PartsCounter partsCounter = new PartsCounter(TMUtil.getParserFromString(content));
        ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
        parseTreeWalker.walk(partsCounter, partsCounter.parser.program());
        return partsCounter.getCount();
    }

    public int getCount() {
        return partsCount;
    }

    @Override
    public void enterArrays(StanParser.ArraysContext ctx) {
        partsCount++;

    }

    @Override
    public void exitArrays(StanParser.ArraysContext ctx) {

    }

    @Override
    public void enterType(StanParser.TypeContext ctx) {

    }

    @Override
    public void exitType(StanParser.TypeContext ctx) {

    }

    @Override
    public void enterInbuilt(StanParser.InbuiltContext ctx) {

    }

    @Override
    public void exitInbuilt(StanParser.InbuiltContext ctx) {

    }

    @Override
    public void enterDim(StanParser.DimContext ctx) {

    }

    @Override
    public void exitDim(StanParser.DimContext ctx) {

    }

    @Override
    public void enterDims(StanParser.DimsContext ctx) {

    }

    @Override
    public void exitDims(StanParser.DimsContext ctx) {

    }

    @Override
    public void enterLimits(StanParser.LimitsContext ctx) {
        partsCount++;

    }

    @Override
    public void exitLimits(StanParser.LimitsContext ctx) {

    }

    @Override
    public void enterDecl(StanParser.DeclContext ctx) {
        partsCount++;

    }

    @Override
    public void exitDecl(StanParser.DeclContext ctx) {

    }

    @Override
    public void enterPrint_stmt(StanParser.Print_stmtContext ctx) {
        partsCount++;

    }

    @Override
    public void exitPrint_stmt(StanParser.Print_stmtContext ctx) {

    }

    @Override
    public void enterFunction_call(StanParser.Function_callContext ctx) {
        partsCount++;

    }

    @Override
    public void exitFunction_call(StanParser.Function_callContext ctx) {

    }

    @Override
    public void enterFunction_call_stmt(StanParser.Function_call_stmtContext ctx) {

    }

    @Override
    public void exitFunction_call_stmt(StanParser.Function_call_stmtContext ctx) {

    }

    @Override
    public void enterAssign_stmt(StanParser.Assign_stmtContext ctx) {
        partsCount++;

    }

    @Override
    public void exitAssign_stmt(StanParser.Assign_stmtContext ctx) {

    }

    @Override
    public void enterArray_access(StanParser.Array_accessContext ctx) {
        partsCount++;

    }

    @Override
    public void exitArray_access(StanParser.Array_accessContext ctx) {

    }

    @Override
    public void enterBlock(StanParser.BlockContext ctx) {

    }

    @Override
    public void exitBlock(StanParser.BlockContext ctx) {

    }

    @Override
    public void enterIf_stmt(StanParser.If_stmtContext ctx) {
        partsCount++;

    }

    @Override
    public void exitIf_stmt(StanParser.If_stmtContext ctx) {

    }

    @Override
    public void enterRange_exp(StanParser.Range_expContext ctx) {

    }

    @Override
    public void exitRange_exp(StanParser.Range_expContext ctx) {

    }

    @Override
    public void enterFor_loop_stmt(StanParser.For_loop_stmtContext ctx) {
        partsCount++;

    }

    @Override
    public void exitFor_loop_stmt(StanParser.For_loop_stmtContext ctx) {

    }

    @Override
    public void enterTarget_stmt(StanParser.Target_stmtContext ctx) {
        partsCount++;

    }

    @Override
    public void exitTarget_stmt(StanParser.Target_stmtContext ctx) {

    }

    @Override
    public void enterDivop(StanParser.DivopContext ctx) {
        partsCount++;

    }

    @Override
    public void exitDivop(StanParser.DivopContext ctx) {

    }

    @Override
    public void enterString(StanParser.StringContext ctx) {

    }

    @Override
    public void exitString(StanParser.StringContext ctx) {

    }

    @Override
    public void enterAddop(StanParser.AddopContext ctx) {
        partsCount++;

    }

    @Override
    public void exitAddop(StanParser.AddopContext ctx) {

    }

    @Override
    public void enterLt(StanParser.LtContext ctx) {
        partsCount++;

    }

    @Override
    public void exitLt(StanParser.LtContext ctx) {

    }

    @Override
    public void enterUnary(StanParser.UnaryContext ctx) {
        partsCount++;

    }

    @Override
    public void exitUnary(StanParser.UnaryContext ctx) {

    }

    @Override
    public void enterInteger(StanParser.IntegerContext ctx) {
        partsCount++;

    }

    @Override
    public void exitInteger(StanParser.IntegerContext ctx) {

    }

    @Override
    public void enterMulop(StanParser.MulopContext ctx) {
        partsCount++;

    }

    @Override
    public void exitMulop(StanParser.MulopContext ctx) {

    }

    @Override
    public void enterArray(StanParser.ArrayContext ctx) {
        partsCount++;

    }

    @Override
    public void exitArray(StanParser.ArrayContext ctx) {

    }

    @Override
    public void enterId_access(StanParser.Id_accessContext ctx) {
        partsCount++;

    }

    @Override
    public void exitId_access(StanParser.Id_accessContext ctx) {

    }

    @Override
    public void enterAnd(StanParser.AndContext ctx) {
        partsCount++;

    }

    @Override
    public void exitAnd(StanParser.AndContext ctx) {

    }

    @Override
    public void enterFunction(StanParser.FunctionContext ctx) {

    }

    @Override
    public void exitFunction(StanParser.FunctionContext ctx) {

    }

    @Override
    public void enterGe(StanParser.GeContext ctx) {
        partsCount++;

    }

    @Override
    public void exitGe(StanParser.GeContext ctx) {

    }

    @Override
    public void enterArray_decl(StanParser.Array_declContext ctx) {

    }

    @Override
    public void exitArray_decl(StanParser.Array_declContext ctx) {

    }

    @Override
    public void enterOr(StanParser.OrContext ctx) {
        partsCount++;

    }

    @Override
    public void exitOr(StanParser.OrContext ctx) {

    }

    @Override
    public void enterExponop(StanParser.ExponopContext ctx) {
        partsCount++;

    }

    @Override
    public void exitExponop(StanParser.ExponopContext ctx) {

    }

    @Override
    public void enterDouble(StanParser.DoubleContext ctx) {
        partsCount++;

    }

    @Override
    public void exitDouble(StanParser.DoubleContext ctx) {

    }

    @Override
    public void enterMinusop(StanParser.MinusopContext ctx) {
        partsCount++;

    }

    @Override
    public void exitMinusop(StanParser.MinusopContext ctx) {

    }

    @Override
    public void enterEq(StanParser.EqContext ctx) {
        partsCount++;

    }

    @Override
    public void exitEq(StanParser.EqContext ctx) {

    }

    @Override
    public void enterGt(StanParser.GtContext ctx) {
        partsCount++;

    }

    @Override
    public void exitGt(StanParser.GtContext ctx) {

    }

    @Override
    public void enterBrackets(StanParser.BracketsContext ctx) {

    }

    @Override
    public void exitBrackets(StanParser.BracketsContext ctx) {

    }

    @Override
    public void enterCondition(StanParser.ConditionContext ctx) {
        partsCount++;

    }

    @Override
    public void exitCondition(StanParser.ConditionContext ctx) {

    }

    @Override
    public void enterNe(StanParser.NeContext ctx) {
        partsCount++;

    }

    @Override
    public void exitNe(StanParser.NeContext ctx) {

    }

    @Override
    public void enterLe(StanParser.LeContext ctx) {
        partsCount++;

    }

    @Override
    public void exitLe(StanParser.LeContext ctx) {

    }

    @Override
    public void enterTranspose(StanParser.TransposeContext ctx) {

    }

    @Override
    public void exitTranspose(StanParser.TransposeContext ctx) {

    }

    @Override
    public void enterTernary_if(StanParser.Ternary_ifContext ctx) {
        partsCount++;

    }

    @Override
    public void exitTernary_if(StanParser.Ternary_ifContext ctx) {

    }

    @Override
    public void enterDistribution_exp(StanParser.Distribution_expContext ctx) {
        partsCount++;

    }

    @Override
    public void exitDistribution_exp(StanParser.Distribution_expContext ctx) {

    }

    @Override
    public void enterSample(StanParser.SampleContext ctx) {
        partsCount++;

    }

    @Override
    public void exitSample(StanParser.SampleContext ctx) {

    }

    @Override
    public void enterReturn_or_param_type(StanParser.Return_or_param_typeContext ctx) {

    }

    @Override
    public void exitReturn_or_param_type(StanParser.Return_or_param_typeContext ctx) {

    }

    @Override
    public void enterParams(StanParser.ParamsContext ctx) {
        partsCount++;

    }

    @Override
    public void exitParams(StanParser.ParamsContext ctx) {

    }

    @Override
    public void enterFunction_decl(StanParser.Function_declContext ctx) {
        partsCount++;
    }

    @Override
    public void exitFunction_decl(StanParser.Function_declContext ctx) {

    }

    @Override
    public void enterReturn_stmt(StanParser.Return_stmtContext ctx) {
        partsCount++;
    }

    @Override
    public void exitReturn_stmt(StanParser.Return_stmtContext ctx) {

    }

    @Override
    public void enterStatement(StanParser.StatementContext ctx) {

    }

    @Override
    public void exitStatement(StanParser.StatementContext ctx) {

    }

    @Override
    public void enterDatablk(StanParser.DatablkContext ctx) {
        partsCount++;

    }

    @Override
    public void exitDatablk(StanParser.DatablkContext ctx) {

    }

    @Override
    public void enterParamblk(StanParser.ParamblkContext ctx) {
        partsCount++;

    }

    @Override
    public void exitParamblk(StanParser.ParamblkContext ctx) {

    }

    @Override
    public void enterModelblk(StanParser.ModelblkContext ctx) {
        partsCount++;

    }

    @Override
    public void exitModelblk(StanParser.ModelblkContext ctx) {

    }

    @Override
    public void enterTransformed_param_blk(StanParser.Transformed_param_blkContext ctx) {
        partsCount++;
    }

    @Override
    public void exitTransformed_param_blk(StanParser.Transformed_param_blkContext ctx) {

    }

    @Override
    public void enterTransformed_data_blk(StanParser.Transformed_data_blkContext ctx) {
        partsCount++;
    }

    @Override
    public void exitTransformed_data_blk(StanParser.Transformed_data_blkContext ctx) {

    }

    @Override
    public void enterGenerated_quantities_blk(StanParser.Generated_quantities_blkContext ctx) {
        partsCount++;
    }

    @Override
    public void exitGenerated_quantities_blk(StanParser.Generated_quantities_blkContext ctx) {

    }

    @Override
    public void enterFunctions_blk(StanParser.Functions_blkContext ctx) {
        partsCount++;
    }

    @Override
    public void exitFunctions_blk(StanParser.Functions_blkContext ctx) {


    }

    @Override
    public void enterProgram(StanParser.ProgramContext ctx) {

    }

    @Override
    public void exitProgram(StanParser.ProgramContext ctx) {

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

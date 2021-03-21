package tool.testmin.util;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;

public class TemplateCounter extends TemplateBaseTransformer {
    private int count;

    private String[] ruleNames;
    private String rule;
    private boolean countComplexDist;
    private String context;
    private boolean enableCount = true;

    public TemplateCounter(String filename, String rule) {
        super(TMUtil.getTemplateParser(filename), null);
        count = 0;
        this.ruleNames = parser.getRuleNames();
        this.rule = rule;
        this.enableCount = true;
    }

    public TemplateCounter(String filename, String rule, boolean countComplexDist) {
        this(filename, rule);
        this.countComplexDist = countComplexDist;
        this.enableCount = true;
    }

    public TemplateCounter(String filename, String rule, String context) {
        this(filename, rule);
        this.context = context;
        this.enableCount = false;
    }

    public static int getCount(String filename, String rule) {
        TemplateCounter counter = new TemplateCounter(filename, rule);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(counter, counter.parser.template());
        return counter.count;
    }

    public static int getCount(String filename, String rule, boolean countComplexDist) {
        TemplateCounter counter = new TemplateCounter(filename, rule, countComplexDist);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(counter, counter.parser.template());
        return counter.count;
    }

    public static int getCount(String filename, String rule, String context) {
        TemplateCounter counter = new TemplateCounter(filename, rule, context);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(counter, counter.parser.template());
        return counter.count;
    }

    @Override
    public void enterDecl(Template2Parser.DeclContext ctx) {
        if (this.context != null && this.context.equals("decl")) {
            this.enableCount = true;
        }
    }

    @Override
    public void exitDecl(Template2Parser.DeclContext ctx) {
        if (this.context != null && this.context.equals("decl")) {
            this.enableCount = false;
        }
    }

    @Override
    public void enterDims(Template2Parser.DimsContext ctx) {
        if (this.context != null && this.context.equals("data_context")) {
            this.enableCount = false;
        }

        if (this.context != null && this.context.equals("dims")) {
            this.enableCount = true;
        }
    }

    @Override
    public void exitDims(Template2Parser.DimsContext ctx) {
        if (this.context != null && this.context.equals("dims")) {
            this.enableCount = false;
        }
    }

    @Override
    public void enterVectorDIMS(Template2Parser.VectorDIMSContext ctx) {
        if (this.context != null && this.context.equals("vectordims")) {
            this.enableCount = true;
        }
    }

    @Override
    public void exitVectorDIMS(Template2Parser.VectorDIMSContext ctx) {
        if (this.context != null && this.context.equals("vectordims")) {
            this.enableCount = false;
        }
    }

    @Override
    public void enterData(Template2Parser.DataContext ctx) {
        if (this.context != null && this.context.equals("data_context")) {
            this.enableCount = true;
        }
    }

    @Override
    public void enterLimits(Template2Parser.LimitsContext ctx) {
        if (this.context != null && this.context.equals("limits")) {
            this.enableCount = true;
        }

        if ("limits".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void exitLimits(Template2Parser.LimitsContext ctx) {
        if (this.context != null && this.context.equals("limits"))
            this.enableCount = false;
    }

    @Override
    public void exitData(Template2Parser.DataContext ctx) {
        if (this.context != null && this.context.equals("data_context")) {
            this.enableCount = false;
        }
    }

    @Override
    public void enterArray_access(Template2Parser.Array_accessContext ctx) {
        if (this.context != null && this.context.equals("array_access")) {
            this.enableCount = true;
        }
    }

    @Override
    public void exitArray_access(Template2Parser.Array_accessContext ctx) {
        if (this.context != null && this.context.equals("array_access")) {
            this.enableCount = false;
        }
    }

    @Override
    public void enterFunctions(Template2Parser.FunctionsContext ctx) {
        if (this.context != null && this.context.equals("functions_context")) {
            this.enableCount = true;
        }
    }

    @Override
    public void exitFunctions(Template2Parser.FunctionsContext ctx) {
        if (this.context != null && this.context.equals("functions_context")) {
            this.enableCount = false;
        }
    }

    @Override
    public void enterTransformeddata(Template2Parser.TransformeddataContext ctx) {
        if (this.context != null && this.context.equals("transformeddata_context")) {
            this.enableCount = true;
        }
    }

    @Override
    public void exitTransformeddata(Template2Parser.TransformeddataContext ctx) {
        if (this.context != null && this.context.equals("transformeddata_context")) {
            this.enableCount = false;
        }
    }

    @Override
    public void enterTransformedparam(Template2Parser.TransformedparamContext ctx) {
        if (this.context != null && this.context.equals("transformedparam_context")) {
            this.enableCount = true;
        }
    }

    @Override
    public void exitTransformedparam(Template2Parser.TransformedparamContext ctx) {
        if (this.context != null && this.context.equals("transformedparam_context")) {
            this.enableCount = false;
        }
    }

    @Override
    public void enterIf_stmt(Template2Parser.If_stmtContext ctx) {
        if ("if_stmt".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterAddop(Template2Parser.AddopContext ctx) {
        if ("addop".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterMinusop(Template2Parser.MinusopContext ctx) {
        if ("minusop".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterMulop(Template2Parser.MulopContext ctx) {
        if ("mulop".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterDivop(Template2Parser.DivopContext ctx) {
        if ("divop".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterExponop(Template2Parser.ExponopContext ctx) {
        if ("exponop".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterDistexpr(Template2Parser.DistexprContext ctx) {
        if ("distribution_exp".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterFunction_call(Template2Parser.Function_callContext ctx) {

        if ("function_call_stmt".equals(rule) && this.enableCount && ctx.getParent() instanceof Template2Parser.StatementContext) {
            count++;
        }
    }

    @Override
    public void enterFunction(Template2Parser.FunctionContext ctx) {
        if ("function_call".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterFor_loop(Template2Parser.For_loopContext ctx) {
        if ("for_loop_stmt".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterPrior(Template2Parser.PriorContext ctx) {
        if ("prior".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterAssign(Template2Parser.AssignContext ctx) {
        if ("assign".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterObserve(Template2Parser.ObserveContext ctx) {
        if ("observe".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterLt(Template2Parser.LtContext ctx) {
        if ("lt".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterLeq(Template2Parser.LeqContext ctx) {
        if ("leq".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterGt(Template2Parser.GtContext ctx) {
        if ("gt".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterGeq(Template2Parser.GeqContext ctx) {
        if ("geq".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterEq(Template2Parser.EqContext ctx) {
        if ("eq".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterNe(Template2Parser.NeContext ctx) {
        if ("ne".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterAnd(Template2Parser.AndContext ctx) {
        if ("and".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterUnary(Template2Parser.UnaryContext ctx) {
        if ("unary".equals(rule) && this.enableCount) {
            count++;
        }
    }

    @Override
    public void enterVal(Template2Parser.ValContext ctx) {
        if ("val".equals(rule) && !ctx.getText().equals("1234.0") && this.enableCount) {
            count++;
        }
        if ("val_all".equals(rule) && this.enableCount && ctx.number().DOUBLE() != null) {
            count++;
        }
    }
}
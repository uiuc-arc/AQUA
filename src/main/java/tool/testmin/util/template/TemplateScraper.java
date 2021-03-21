package tool.testmin.util.template;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;
import tool.testmin.util.TMUtil;

import java.util.ArrayList;

public class TemplateScraper extends TemplateBaseTransformer {
    private final String[] ruleNames;
    boolean enableScraping;
    private ArrayList<ParserRuleContext> rules;
    private String context;
    private String rule;

    public TemplateScraper(String testfile, String rule, String context) {
        this(TMUtil.getTemplateParser(testfile), rule, context);
    }

    public TemplateScraper(Template2Parser parser, String rule, String context) {
        super(parser, null);
        if (context != null && !context.isEmpty()) {
            enableScraping = false;
        } else {
            context = "";
            enableScraping = true;
        }

        this.rules = new ArrayList<>();
        this.ruleNames = parser.getRuleNames();
        this.context = context;
        this.rule = rule;
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, this.parser.template());
    }

    public static ArrayList<ParserRuleContext> scrape(String testfile, String rule, String context) {
        TemplateScraper TemplateScraper = new TemplateScraper(testfile, rule, context);
        return TemplateScraper.getRules();
    }

    public static ArrayList<ParserRuleContext> scrape(Template2Parser parser, String rule, String context) {
        TemplateScraper TemplateScraper = new TemplateScraper(parser, rule, context);
        return TemplateScraper.getRules();
    }

    public ArrayList<ParserRuleContext> getRules() {
        return rules;
    }

    @Override
    public void enterPrimitive(Template2Parser.PrimitiveContext ctx) {
        super.enterPrimitive(ctx);
    }

    @Override
    public void exitPrimitive(Template2Parser.PrimitiveContext ctx) {
        super.exitPrimitive(ctx);
    }

    @Override
    public void enterNumber(Template2Parser.NumberContext ctx) {
        super.enterNumber(ctx);
    }

    @Override
    public void exitNumber(Template2Parser.NumberContext ctx) {
        super.exitNumber(ctx);
    }

    @Override
    public void enterDtype(Template2Parser.DtypeContext ctx) {
        super.enterDtype(ctx);
    }

    @Override
    public void exitDtype(Template2Parser.DtypeContext ctx) {
        super.exitDtype(ctx);
    }

    @Override
    public void enterArray(Template2Parser.ArrayContext ctx) {
        super.enterArray(ctx);
    }

    @Override
    public void exitArray(Template2Parser.ArrayContext ctx) {
        super.exitArray(ctx);
    }

    @Override
    public void enterVector(Template2Parser.VectorContext ctx) {
        super.enterVector(ctx);
    }

    @Override
    public void exitVector(Template2Parser.VectorContext ctx) {
        super.exitVector(ctx);
    }

    @Override
    public void enterDims(Template2Parser.DimsContext ctx) {
        super.enterDims(ctx);
    }

    @Override
    public void exitDims(Template2Parser.DimsContext ctx) {
        super.exitDims(ctx);
    }

    @Override
    public void enterVectorDIMS(Template2Parser.VectorDIMSContext ctx) {
        super.enterVectorDIMS(ctx);
    }

    @Override
    public void exitVectorDIMS(Template2Parser.VectorDIMSContext ctx) {
        super.exitVectorDIMS(ctx);
    }

    @Override
    public void enterDecl(Template2Parser.DeclContext ctx) {
        if (rule.equals("decl")) {
            if (enableScraping) {
                rules.add(ctx);
            }
        }
    }

    @Override
    public void enterGeneratedquantities(Template2Parser.GeneratedquantitiesContext ctx) {
        if (this.context != null && this.context.equals("generatedquantities")) {
            this.enableScraping = true;
        }
    }

    @Override
    public void exitGeneratedquantities(Template2Parser.GeneratedquantitiesContext ctx) {
        if (this.context != null && this.context.equals("generatedquantities")) {
            this.enableScraping = false;
        }
    }

    @Override
    public void exitDecl(Template2Parser.DeclContext ctx) {
        super.exitDecl(ctx);
    }

    @Override
    public void enterLimits(Template2Parser.LimitsContext ctx) {
        super.enterLimits(ctx);
    }

    @Override
    public void exitLimits(Template2Parser.LimitsContext ctx) {
        super.exitLimits(ctx);
    }

    @Override
    public void enterData(Template2Parser.DataContext ctx) {
        if (rule.equals("data")) {
            if (enableScraping) {
                rules.add(ctx);
            }
        }

        if (this.context != null && this.context.equals("data")) {
            this.enableScraping = true;
        }
    }

    @Override
    public void exitData(Template2Parser.DataContext ctx) {
        if (this.context != null && this.context.equals("data"))
            this.enableScraping = false;
    }

    @Override
    public void enterPrior(Template2Parser.PriorContext ctx) {
        if (rule.equals("prior")) {
            if (enableScraping) {
                rules.add(ctx);
            }
        } else if (rule.equals("1d_prior")) {
            if (ctx.distexpr().vectorDIMS() == null)
                rules.add(ctx);
        }
    }

    @Override
    public void exitPrior(Template2Parser.PriorContext ctx) {
        super.exitPrior(ctx);
    }

    @Override
    public void enterParam(Template2Parser.ParamContext ctx) {
        super.enterParam(ctx);
    }

    @Override
    public void exitParam(Template2Parser.ParamContext ctx) {
        super.exitParam(ctx);
    }

    @Override
    public void enterParams(Template2Parser.ParamsContext ctx) {
        super.enterParams(ctx);
    }

    @Override
    public void exitParams(Template2Parser.ParamsContext ctx) {
        super.exitParams(ctx);
    }

    @Override
    public void enterDistexpr(Template2Parser.DistexprContext ctx) {
        if (rule.equals("cont_dist")) {
            if (ctx.getText().contains("dirichlet") || ctx.getText().contains("1234.0"))
                super.enterDistexpr(ctx);
            else
                rules.add(ctx);
        }
    }

    @Override
    public void exitDistexpr(Template2Parser.DistexprContext ctx) {
        super.exitDistexpr(ctx);
    }

    @Override
    public void enterLoopcomp(Template2Parser.LoopcompContext ctx) {
        super.enterLoopcomp(ctx);
    }

    @Override
    public void exitLoopcomp(Template2Parser.LoopcompContext ctx) {
        super.exitLoopcomp(ctx);
    }

    @Override
    public void enterFor_loop(Template2Parser.For_loopContext ctx) {
        super.enterFor_loop(ctx);
    }

    @Override
    public void exitFor_loop(Template2Parser.For_loopContext ctx) {
        super.exitFor_loop(ctx);
    }

    @Override
    public void enterIf_stmt(Template2Parser.If_stmtContext ctx) {
        super.enterIf_stmt(ctx);
    }

    @Override
    public void exitIf_stmt(Template2Parser.If_stmtContext ctx) {
        super.exitIf_stmt(ctx);
    }

    @Override
    public void enterElse_blk(Template2Parser.Else_blkContext ctx) {
        super.enterElse_blk(ctx);
    }

    @Override
    public void exitElse_blk(Template2Parser.Else_blkContext ctx) {
        super.exitElse_blk(ctx);
    }

    @Override
    public void enterFunction_call(Template2Parser.Function_callContext ctx) {
        if (rule.equals("func")) {
            String func_name = ctx.FUNCTION().getText();
            if (!func_name.contains("rng")
                    && !func_name.contains("prob")
                    && !func_name.contains("softmax")
                    && !func_name.contains("log_sum_exp")) {
                rules.add(ctx);
            }
        }
        super.enterFunction_call(ctx);
    }

    @Override
    public void exitFunction_call(Template2Parser.Function_callContext ctx) {
        super.exitFunction_call(ctx);
    }

    @Override
    public void enterBlock(Template2Parser.BlockContext ctx) {
        super.enterBlock(ctx);
    }

    @Override
    public void exitBlock(Template2Parser.BlockContext ctx) {
        super.exitBlock(ctx);
    }

    @Override
    public void enterTransformedparam(Template2Parser.TransformedparamContext ctx) {
        if (this.context != null && this.context.equals("transformedparam"))
            this.enableScraping = true;
    }

    @Override
    public void exitTransformedparam(Template2Parser.TransformedparamContext ctx) {
        if (this.context != null && this.context.equals("transformedparam"))
            this.enableScraping = false;
    }

    @Override
    public void enterTransformeddata(Template2Parser.TransformeddataContext ctx) {
        if (this.context != null && this.context.equals("transformeddata"))
            this.enableScraping = true;
        else
            super.enterTransformeddata(ctx);
    }

    @Override
    public void exitTransformeddata(Template2Parser.TransformeddataContext ctx) {
        if (this.context != null && this.context.equals("transformeddata"))
            this.enableScraping = false;
        else
            super.exitTransformeddata(ctx);
    }

    @Override
    public void enterVal(Template2Parser.ValContext ctx) {
        super.enterVal(ctx);
    }

    @Override
    public void exitVal(Template2Parser.ValContext ctx) {
        super.exitVal(ctx);
    }

    @Override
    public void enterDivop(Template2Parser.DivopContext ctx) {
        if (rule.equals("divop")) {
            if (enableScraping) {
                rules.add(ctx);
            }
        }
    }

    @Override
    public void exitDivop(Template2Parser.DivopContext ctx) {
        super.exitDivop(ctx);
    }

    @Override
    public void enterExponop(Template2Parser.ExponopContext ctx) {
        if (rule.equals("exponop")) {
            if (enableScraping) {
                rules.add(ctx);
            }
        }
    }

    @Override
    public void exitExponop(Template2Parser.ExponopContext ctx) {
        super.exitExponop(ctx);
    }

    @Override
    public void enterArray_access(Template2Parser.Array_accessContext ctx) {
        if (rule.equals("array_access")) {
            if (enableScraping) {
                rules.add(ctx);
            }
        }
    }

    @Override
    public void exitArray_access(Template2Parser.Array_accessContext ctx) {
        super.exitArray_access(ctx);
    }

    @Override
    public void enterAddop(Template2Parser.AddopContext ctx) {
        if (rule.equals("addop")) {
            if (enableScraping) {
                rules.add(ctx);
            }
        }
    }

    @Override
    public void exitAddop(Template2Parser.AddopContext ctx) {
        super.exitAddop(ctx);
    }

    @Override
    public void enterMinusop(Template2Parser.MinusopContext ctx) {
        if (rule.equals("minusop")) {
            if (enableScraping) {
                rules.add(ctx);
            }
        }
    }

    @Override
    public void exitMinusop(Template2Parser.MinusopContext ctx) {
        super.exitMinusop(ctx);
    }

    @Override
    public void enterLt(Template2Parser.LtContext ctx) {
        super.enterLt(ctx);
    }

    @Override
    public void exitLt(Template2Parser.LtContext ctx) {
        super.exitLt(ctx);
    }

    @Override
    public void enterUnary(Template2Parser.UnaryContext ctx) {
        super.enterUnary(ctx);
    }

    @Override
    public void exitUnary(Template2Parser.UnaryContext ctx) {
        super.exitUnary(ctx);
    }

    @Override
    public void enterEq(Template2Parser.EqContext ctx) {
        super.enterEq(ctx);
    }

    @Override
    public void exitEq(Template2Parser.EqContext ctx) {
        super.exitEq(ctx);
    }

    @Override
    public void enterGt(Template2Parser.GtContext ctx) {
        super.enterGt(ctx);
    }

    @Override
    public void exitGt(Template2Parser.GtContext ctx) {
        super.exitGt(ctx);
    }

    @Override
    public void enterBrackets(Template2Parser.BracketsContext ctx) {
        super.enterBrackets(ctx);
    }

    @Override
    public void exitBrackets(Template2Parser.BracketsContext ctx) {
        super.exitBrackets(ctx);
    }

    @Override
    public void enterRef(Template2Parser.RefContext ctx) {
        if (rule.equals("ref")) {
            if (enableScraping) {
                rules.add(ctx);
            }
        }
    }

    @Override
    public void exitRef(Template2Parser.RefContext ctx) {
        super.exitRef(ctx);
    }

    @Override
    public void enterGeq(Template2Parser.GeqContext ctx) {
        super.enterGeq(ctx);
    }

    @Override
    public void exitGeq(Template2Parser.GeqContext ctx) {
        super.exitGeq(ctx);
    }

    @Override
    public void enterMulop(Template2Parser.MulopContext ctx) {
        if (rule.equals("mulop")) {
            if (enableScraping) {
                rules.add(ctx);
            }
        }
    }

    @Override
    public void exitMulop(Template2Parser.MulopContext ctx) {
        super.exitMulop(ctx);
    }

    @Override
    public void enterAnd(Template2Parser.AndContext ctx) {
        super.enterAnd(ctx);
    }

    @Override
    public void exitAnd(Template2Parser.AndContext ctx) {
        super.exitAnd(ctx);
    }

    @Override
    public void enterFunction(Template2Parser.FunctionContext ctx) {
        super.enterFunction(ctx);
    }

    @Override
    public void exitFunction(Template2Parser.FunctionContext ctx) {
        super.exitFunction(ctx);
    }

    @Override
    public void enterNe(Template2Parser.NeContext ctx) {
        super.enterNe(ctx);
    }

    @Override
    public void exitNe(Template2Parser.NeContext ctx) {
        super.exitNe(ctx);
    }

    @Override
    public void enterLeq(Template2Parser.LeqContext ctx) {
        super.enterLeq(ctx);
    }

    @Override
    public void exitLeq(Template2Parser.LeqContext ctx) {
        super.exitLeq(ctx);
    }

    @Override
    public void enterTernary(Template2Parser.TernaryContext ctx) {
        super.enterTernary(ctx);
    }

    @Override
    public void exitTernary(Template2Parser.TernaryContext ctx) {
        super.exitTernary(ctx);
    }

    @Override
    public void enterAssign(Template2Parser.AssignContext ctx) {
        if (rule.equals("assign")) {
            if (enableScraping) {
                rules.add(ctx);
            }
        }
    }

    @Override
    public void exitAssign(Template2Parser.AssignContext ctx) {
        super.exitAssign(ctx);
    }

    @Override
    public void enterObserve(Template2Parser.ObserveContext ctx) {
        super.enterObserve(ctx);
    }

    @Override
    public void exitObserve(Template2Parser.ObserveContext ctx) {
        super.exitObserve(ctx);
    }

    @Override
    public void enterStatement(Template2Parser.StatementContext ctx) {
        super.enterStatement(ctx);
    }

    @Override
    public void exitStatement(Template2Parser.StatementContext ctx) {
        super.exitStatement(ctx);
    }

    @Override
    public void enterQuery(Template2Parser.QueryContext ctx) {
        super.enterQuery(ctx);
    }

    @Override
    public void exitQuery(Template2Parser.QueryContext ctx) {
        super.exitQuery(ctx);
    }

    @Override
    public void enterTemplate(Template2Parser.TemplateContext ctx) {
        super.enterTemplate(ctx);
    }

    @Override
    public void exitTemplate(Template2Parser.TemplateContext ctx) {
        super.exitTemplate(ctx);
    }
}

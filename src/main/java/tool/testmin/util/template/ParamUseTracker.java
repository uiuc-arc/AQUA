package tool.testmin.util.template;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;
import tool.testmin.util.MyLogger;
import tool.testmin.util.TMUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ParamUseTracker extends TemplateBaseTransformer {
    private final Map<String, SymbolInfo> parameterMap;
    private String currentBlock = "model";

    public ParamUseTracker(Template2Parser parser, MyLogger logger, Map<String, SymbolInfo> parameterMap) {
        super(parser, logger);
        this.parameterMap = parameterMap;
        ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
        parseTreeWalker.walk(this, parser.template());
    }

    public static void GetUnusedParams(String filename) {
        Map<String, SymbolInfo> parameterMap = new HashMap<>();

        ArrayList<ParserRuleContext> parameters = TemplateScraper.scrape(TMUtil.getTemplateParser(filename), "prior", "");
        ArrayList<ParserRuleContext> transformedparameters = TemplateScraper.scrape(TMUtil.getTemplateParser(filename), "decl", "transformedparam");
        ArrayList<ParserRuleContext> generatedquantities = TemplateScraper.scrape(TMUtil.getTemplateParser(filename), "decl", "generatedquantities");

        for (ParserRuleContext prc : parameters) {

            SymbolInfo symbolInfo = new SymbolInfo(prc);
            parameterMap.put(symbolInfo.id, symbolInfo);
        }

        for (ParserRuleContext prc : transformedparameters) {

            SymbolInfo symbolInfo = new SymbolInfo(prc);
            parameterMap.put(symbolInfo.id, symbolInfo);
        }

        ParamUseTracker paramUseTracker = new ParamUseTracker(TMUtil.getTemplateParser(filename), null, parameterMap);

        for (SymbolInfo symbolInfo : parameterMap.values()) {
            if (symbolInfo.usedInModelBlock) {
                System.out.println("+" + symbolInfo.id);
            } else {
                if (symbolInfo.usedDims.size() > 0) {
                    if (symbolInfo.allConstantDims) {

                        if (symbolInfo.usedDims.size() > 0) {
                            for (Template2Parser.DimsContext dimsContext : symbolInfo.usedDims) {
                                System.out.println("+" + symbolInfo.id + "." + dimsContext.getText().replaceAll(",", "."));
                            }
                        }
                    } else {

                        System.out.println("+" + symbolInfo.id);
                    }
                } else {
                    System.out.println("-" + symbolInfo.id);
                }
            }
        }

        for (ParserRuleContext prc : generatedquantities) {
            System.out.println("-" + ((Template2Parser.DeclContext) prc).ID().getText());
        }
    }

    private boolean isLHS(ParserRuleContext node, ParserRuleContext parent) {
        if (parent != null && (parent instanceof Template2Parser.AssignContext || parent instanceof Template2Parser.PriorContext)) {

            return parent.children.indexOf(node) == 0;
        } else return parent != null && parent instanceof Template2Parser.DeclContext;

    }

    private void updateUse(String id) {
        if (parameterMap.containsKey(id)) {
            SymbolInfo symbolInfo = parameterMap.get(id);
            symbolInfo.usedInModelBlock = true;
        }
    }

    private void updateUsedDims(String id, Template2Parser.DimsContext dimsContext) {
        if (parameterMap.containsKey(id)) {
            SymbolInfo symbolInfo = parameterMap.get(id);
            symbolInfo.usedDims.add(dimsContext);
            for (Template2Parser.ExprContext exprContext : dimsContext.expr()) {
                if (!(exprContext instanceof Template2Parser.ValContext)) {
                    symbolInfo.allConstantDims = false;
                }
            }
        }
    }

    @Override
    public void enterRef(Template2Parser.RefContext ctx) {
        if (currentBlock.equals("model") && !isLHS(ctx, ctx.getParent())) {
            updateUse(ctx.ID().getText());
        }
    }

    @Override
    public void enterArray_access(Template2Parser.Array_accessContext ctx) {
        if (currentBlock.equals("model")) {
            if (!isLHS(ctx, ctx.getParent())) {

                updateUsedDims(ctx.ID().getText(), ctx.dims());
            }
        }
    }

    @Override
    public void enterTransformeddata(Template2Parser.TransformeddataContext ctx) {
        currentBlock = "transformeddata";
    }

    @Override
    public void exitTransformeddata(Template2Parser.TransformeddataContext ctx) {
        currentBlock = "model";
    }

    @Override
    public void enterTransformedparam(Template2Parser.TransformedparamContext ctx) {
        currentBlock = "transformedparam";
    }

    @Override
    public void exitTransformedparam(Template2Parser.TransformedparamContext ctx) {
        currentBlock = "model";
    }

    @Override
    public void enterGeneratedquantities(Template2Parser.GeneratedquantitiesContext ctx) {
        currentBlock = "generatedquantities";
    }

    @Override
    public void exitGeneratedquantities(Template2Parser.GeneratedquantitiesContext ctx) {
        currentBlock = "model";
    }

    @Override
    public void enterQuery(Template2Parser.QueryContext ctx) {
        currentBlock = "query";
    }

    @Override
    public void exitQuery(Template2Parser.QueryContext ctx) {
        currentBlock = "model";
    }

    @Override
    public void enterFunctions(Template2Parser.FunctionsContext ctx) {
        currentBlock = "functions";
    }

    @Override
    public void exitFunctions(Template2Parser.FunctionsContext ctx) {
        currentBlock = "model";
    }

    @Override
    public void enterPrior(Template2Parser.PriorContext ctx) {
        super.enterPrior(ctx);
    }
}

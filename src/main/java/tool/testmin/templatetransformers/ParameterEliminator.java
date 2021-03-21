package tool.testmin.templatetransformers;


import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;
import tool.testmin.util.MyLogger;
import tool.testmin.util.TestFile;
import tool.testmin.util.TMUtil;
import tool.testmin.util.template.ParamReplacer;
import tool.testmin.util.template.TemplateDimensionChecker;
import tool.testmin.util.template.TemplateScraper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static tool.testmin.util.TMUtil.getParamName;

public class ParameterEliminator extends TemplateBaseTransformer {

    private static boolean InBlock = false;
    private static boolean InSample = false;
    private boolean InParamBlk;
    private String paramName;
    private String value;
    private String dims;
    private String[] rulesNames;
    private int counter;
    private boolean modifiedTree;
    private String program;
    private MyLogger logger;


    public ParameterEliminator(Template2Parser parser, MyLogger logger) {
        super(parser, logger);
        this.logger = logger;
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));

                ArrayList<ParserRuleContext> priors = TemplateScraper.scrape(testfile, "prior", null);
                logger.print("Parameters: " + priors.size(), true);
                for (int l = 0; l < priors.size(); ) {
                    ArrayList<ParserRuleContext> curPriors = TemplateScraper.scrape(testfile, "prior", null);
                    if (l > curPriors.size() - 1)
                        break;
                    ParamReplacer paramReplacer = new ParamReplacer(TMUtil.getTemplateParserFromString(filecontent),
                            logger,
                            getParamName(curPriors.get(l)),
                            l + 1);
                    if (paramReplacer.value == null) {
                        return false;
                    }
                    ParameterEliminator parameterEliminator = new ParameterEliminator(TMUtil.getTemplateParserFromString(filecontent), logger);
                    parameterEliminator.counter = l + 1;
                    parameterEliminator.paramName = getParamName(curPriors.get(l));
                    parameterEliminator.program = filecontent;
                    parameterEliminator.value = paramReplacer.value;

                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(parameterEliminator, parameterEliminator.parser.template());
                    String newContent = parameterEliminator.rewriter.getText();
                    logger.print(newContent, true);
                    if (parameterEliminator.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(ParameterEliminator.class),
                                    logger,
                                    testFile)) {

                        filecontent = newContent;
                        changed = true;
                        logger.print("Changed", true);
                    } else {
                        l++;
                    }
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return changed;
    }

    @Override
    public void enterTemplate(Template2Parser.TemplateContext ctx) {
        print("Replacing parameter " + this.paramName);
    }

    private String getSubstitute(Template2Parser.DimsContext dim, String value) {
        String res = String.format("rep_val(%s,", value);
        for (Template2Parser.ExprContext d : dim.expr()) {
            res += d.getText() + ",";
        }
        return res.substring(0, res.length() - 1) + ")";
    }

    @Override
    public void exitRef(Template2Parser.RefContext ctx) {

        if (ctx.ID().getText().equals(this.paramName) && !(ctx.getParent() instanceof Template2Parser.Array_accessContext) && !(ctx.getParent() instanceof Template2Parser.PriorContext)) {


            if (this.value == null) {
                print("No replacement found: " + ctx.getText());
                return;
            }
            print("removing ref");
            String replacement = TemplateDimensionChecker.getSubstitute(program, ctx, this.value);

            print("Replacement param: " + replacement);
            if (replacement != null) {
                this.rewriter.replace(ctx.getStart(), ctx.getStop(), replacement);
                this.modifiedTree = true;
            }
        }
    }

    @Override
    public void exitArray_access(Template2Parser.Array_accessContext ctx) {
        if (ctx.ID().getText().equals(this.paramName) && !(ctx.getParent() instanceof Template2Parser.PriorContext)) {

            int len = ctx.dims().expr().size();

            if (this.value == null) {
                print("No replacement found: " + ctx.getText());
                return;
            }
            print("removing array access");

            String replacement = TemplateDimensionChecker.getSubstitute(program, ctx, this.value);
            print("Replacement param: " + replacement);

            if (replacement != null) {
                this.rewriter.replace(ctx.getStart(), ctx.getStop(), replacement);
                this.modifiedTree = true;
            }
        }
    }

    @Override
    public void enterPrior(Template2Parser.PriorContext ctx) {
        counter--;
        if (counter == 0) {
            if (this.value != null) {
                this.rewriter.delete(ctx.getStart(), ctx.getStop());
                this.modifiedTree = true;
            }
        }
    }


}

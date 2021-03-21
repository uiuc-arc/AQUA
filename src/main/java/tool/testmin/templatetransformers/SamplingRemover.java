package tool.testmin.templatetransformers;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;
import tool.testmin.util.MyLogger;
import tool.testmin.util.TemplateCounter;
import tool.testmin.util.TestFile;
import tool.testmin.util.TMUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SamplingRemover extends TemplateBaseTransformer {
    private int sample;
    private boolean detmode;
    private boolean modifiedTree;

    public SamplingRemover(Template2Parser parser, MyLogger logger) {
        super(parser, logger);
    }

    public SamplingRemover(Template2Parser parserFromString, boolean b, int l, MyLogger logger) {
        super(parserFromString, logger);
        this.detmode = b;
        this.sample = l;
        new ParseTreeWalker().walk(this, this.parser.template());
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int sample = TemplateCounter.getCount(testfile, "prior");
                logger.print("Samples: " + sample, true);
                for (int l = 0; l < sample; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    SamplingRemover conditionalEliminator = new SamplingRemover(parser, logger);
                    conditionalEliminator.sample = l + 1;
                    conditionalEliminator.detmode = true;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(conditionalEliminator, conditionalEliminator.parser.template());
                    String newContent = conditionalEliminator.rewriter.getText();
                    if (conditionalEliminator.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(SamplingRemover.class),
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

    public boolean isModified() {
        return modifiedTree;
    }

    public String getText() {
        return this.rewriter.getText();
    }

    private String getReplacement(Template2Parser.PriorContext ctx) {
        String rep = "normal";
        if (ctx.distexpr().ID().getText().equals("dirichlet"))
            rep = "dirichlet";
        if (ctx.distexpr().vectorDIMS() != null && ctx.distexpr().vectorDIMS().dims().expr().size() > 0) {
            rep += ctx.distexpr().vectorDIMS().getText();
        }
        if (rep.equals("normal"))
            rep += "(1234.0, 1234.0)";
        else {
            assert rep.contains("dirichlet");
            rep += "(1234.0)";
        }
        if (ctx.distexpr().dims() != null) {
            rep += "[" + ctx.distexpr().dims().getText() + "]";
        }
        return rep;
    }

    @Override
    public void exitPrior(Template2Parser.PriorContext ctx) {
        sample--;
        if (sample == 0) {
            print("Removing Sample Stmt: " + TMUtil.getStringFromContext(ctx));
            ParserRuleContext parent = ctx.getParent().getParent();
            if (parent.getRuleIndex() == Template2Parser.RULE_block && parent.children.size() == 1) {
                print("Rule Block... only child");
                String rep = getReplacement(ctx);
                this.rewriter.replace(ctx.distexpr().getStart(), ctx.distexpr().getStop(), "{" + rep + "}");

                modifiedTree = true;
            } else {
                String rep = getReplacement(ctx);
                this.rewriter.replace(ctx.distexpr().getStart(), ctx.distexpr().getStop(), rep);

                modifiedTree = true;
            }

        }
    }


}

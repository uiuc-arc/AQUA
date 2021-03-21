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

public class ObserveRemover extends TemplateBaseTransformer {
    private int observe;
    private boolean detmode;
    private boolean modifiedTree;

    public ObserveRemover(Template2Parser parser, MyLogger logger) {
        super(parser, logger);
    }

    public ObserveRemover(Template2Parser parserFromString, boolean b, int l, MyLogger logger) {
        super(parserFromString, logger);
        this.detmode = b;
        this.observe = l;
        new ParseTreeWalker().walk(this, this.parser.template());
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int observe = TemplateCounter.getCount(testfile, "observe");
                logger.print("Observes: " + observe, true);
                for (int l = 0; l < observe; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    ObserveRemover observeRemover = new ObserveRemover(parser, logger);
                    observeRemover.observe = l + 1;
                    observeRemover.detmode = true;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(observeRemover, observeRemover.parser.template());
                    String newContent = observeRemover.rewriter.getText();
                    if (observeRemover.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(ObserveRemover.class),
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

    @Override
    public void exitObserve(Template2Parser.ObserveContext ctx) {
        observe--;
        if (observe == 0) {
            print("Removing Observe Stmt: " + TMUtil.getStringFromContext(ctx));
            ParserRuleContext parent = ctx.getParent().getParent();
            if (parent.getRuleIndex() == Template2Parser.RULE_block && parent.children.size() == 1) {
                print("Rule Block... only child");
                this.rewriter.replace(ctx.getStart(), ctx.getStop(), "{ }");
                modifiedTree = true;
            } else {
                this.rewriter.delete(ctx.getStart(), ctx.getStop());
                modifiedTree = true;
            }
        }
    }

}

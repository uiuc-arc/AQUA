package tool.testmin.templatetransformers;


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

public class LimitsRemover extends TemplateBaseTransformer {
    private int limit;
    private boolean modifiedTree;
    private String program;

    public LimitsRemover(Template2Parser parser, MyLogger logger) {
        super(parser, logger);
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int limits = TemplateCounter.getCount(testfile, "limits");
                for (int l = 0; l < limits; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    LimitsRemover limitsRemover = new LimitsRemover(parser, logger);
                    limitsRemover.limit = l + 1;
                    limitsRemover.program = filecontent;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(limitsRemover, limitsRemover.parser.template());
                    String newContent = limitsRemover.rewriter.getText();
                    if (limitsRemover.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(LimitsRemover.class),
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
    public void exitLimits(Template2Parser.LimitsContext ctx) {
        limit--;
        if (limit == 0) {
            this.rewriter.delete(ctx.getStart(), ctx.getStop());
            print("Removed limits ...");
            modifiedTree = true;
        }
    }


}


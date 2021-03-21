package tool.testmin.templatetransformers;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
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

public class LoopVarRemover extends TemplateBaseTransformer {

    private int counter = 0;
    private boolean modifiedTree;

    public LoopVarRemover(Template2Parser parser, MyLogger logger) {
        super(parser, logger);
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int loops = TemplateCounter.getCount(testfile, "for_loop_stmt");
                for (int l = 0; l < loops; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    LoopVarRemover loopVarRemover = new LoopVarRemover(parser, logger);
                    loopVarRemover.counter = l + 1;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(loopVarRemover, loopVarRemover.parser.template());
                    String newContent = loopVarRemover.rewriter.getText();
                    if (loopVarRemover.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(LoopVarRemover.class),
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
    public void exitFor_loop(Template2Parser.For_loopContext ctx) {
        counter--;
        if (counter == 0) {
            String loop_var = ctx.ID().getText();
            String code = ctx.start.getInputStream().getText(new Interval(ctx.block().start.getStartIndex(), ctx.block().stop.getStopIndex()));
            ParserRuleContext start = ctx.expr(0);
            if (start instanceof Template2Parser.ValContext) {

                print("Using existing index:" + start.getText());
                code = code.replaceAll("\\b" + loop_var + "\\b", start.getText());
            } else {

                code = code.replaceAll("\\b" + loop_var + "\\b", "1");
            }

            code = code.trim();
            if (code.startsWith("{")) {
                code = code.substring(1);
            }
            if (code.endsWith("}")) {
                code = code.substring(0, code.length() - 1);
            }

            print("Replaced loop variable : " + code);
            this.rewriter.replace(ctx.getStart(), ctx.getStop(), code);

            print("Eliminiated loop...");
            this.modifiedTree = true;
        }
    }


}

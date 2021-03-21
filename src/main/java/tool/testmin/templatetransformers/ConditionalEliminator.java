package tool.testmin.templatetransformers;

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

public class ConditionalEliminator extends TemplateBaseTransformer {
    private boolean branch;
    private int counter;
    private boolean detmode;
    private boolean modifiedTree;

    public ConditionalEliminator(Template2Parser parser, MyLogger logger, boolean branch) {
        super(parser, logger);
        this.branch = branch;
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int ifstmts = TemplateCounter.getCount(testfile, "if_stmt");
                logger.print("If statements: " + ifstmts, true);
                boolean branch_first = true;
                for (int l = 0; l < ifstmts; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    ConditionalEliminator conditionalEliminator = new ConditionalEliminator(parser, logger, branch_first);
                    conditionalEliminator.counter = l + 1;
                    conditionalEliminator.detmode = true;

                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(conditionalEliminator, conditionalEliminator.parser.template());
                    String newContent = conditionalEliminator.rewriter.getText();
                    if (conditionalEliminator.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(ConditionalEliminator.class),
                                    logger,
                                    testFile)) {

                        filecontent = newContent;
                        changed = true;
                        logger.print("Changed", true);
                    } else {
                        if (branch_first) {
                            branch_first = false;
                        } else {

                            l++;
                            branch_first = true;
                        }
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
    public void exitIf_stmt(Template2Parser.If_stmtContext ctx) {
        String innerBlock = "";
        if (detmode) {
            counter--;
            if (counter == 0) {
                Template2Parser.BlockContext blk = null;
                if (ctx.else_blk() != null) {
                    blk = this.branch ? ctx.block() : ctx.else_blk().block();
                    for (Template2Parser.StatementContext s : blk.statement()) {
                        innerBlock += s.getText() + '\n';
                    }
                    if (blk == ctx.block())
                        print("Replaced with true branch");
                    else
                        print("Replaced with false branch");
                } else {
                    blk = ctx.block();
                    for (Template2Parser.StatementContext s : ctx.block().statement()) {
                        innerBlock += s.getText() + '\n';
                    }
                    print("Replaced with true branch");
                }
                String code = ctx.start.getInputStream().getText(new Interval(blk.start.getStartIndex(), blk.stop.getStopIndex()));
                code = code.trim();
                if (code.startsWith("{")) {
                    code = code.substring(1);
                }
                if (code.endsWith("}")) {
                    code = code.substring(0, code.length() - 1);
                }
                rewriter.replace(ctx.start, ctx.stop, code);
                modifiedTree = true;
            }
        }
    }


}

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

public class FunctionCallStmtEliminator extends TemplateBaseTransformer {


    private boolean remove = true;
    private boolean modifiedTree;
    private int counter = 0;
    private boolean detMode;

    public FunctionCallStmtEliminator(String testfile, MyLogger logger) {
        super(TMUtil.getTemplateParser(testfile), logger);
        print("Running function call stmt eliminator");
    }

    public FunctionCallStmtEliminator(Template2Parser parser, MyLogger logger) {
        super(parser, logger);
    }

    public static String Transform(String testfile) {
        FunctionCallStmtEliminator functionCallStmtEliminator = new FunctionCallStmtEliminator(testfile, null);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(functionCallStmtEliminator, functionCallStmtEliminator.parser.template());
        return functionCallStmtEliminator.rewriter.getText();
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int function_call_stmts = TemplateCounter.getCount(testfile, "function_call_stmt");
                logger.print("function calls: " + function_call_stmts, true);
                for (int l = 0; l < function_call_stmts; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    FunctionCallStmtEliminator functionCallStmtEliminator = new FunctionCallStmtEliminator(parser, logger);
                    functionCallStmtEliminator.counter = l + 1;
                    functionCallStmtEliminator.detMode = true;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(functionCallStmtEliminator, functionCallStmtEliminator.parser.template());
                    String newContent = functionCallStmtEliminator.rewriter.getText();
                    if (functionCallStmtEliminator.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(FunctionCallStmtEliminator.class),
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
    public void exitFunction_call(Template2Parser.Function_callContext ctx) {
        if (detMode) {
            if (!(ctx.getParent() instanceof Template2Parser.StatementContext))
                return;
            counter--;
            if (ctx.FUNCTION().getText().equals("return"))
                return;

            if (counter == 0) {
                ParserRuleContext parent = ctx.getParent().getParent();

                if (parent.getRuleIndex() == Template2Parser.RULE_block && parent.children.size() == 1) {
                    this.rewriter.replace(ctx.getStart(), ctx.getStop(), "{ }");
                } else {
                    this.rewriter.delete(ctx.getStart(), ctx.getStop());
                }

                print("Removed " + ctx.FUNCTION().getText());
                modifiedTree = true;
            }
        }
    }


}

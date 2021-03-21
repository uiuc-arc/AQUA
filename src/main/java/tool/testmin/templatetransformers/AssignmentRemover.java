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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AssignmentRemover extends TemplateBaseTransformer {
    private static Map<String, ArrayList<String>> failedTransformations;

    static {
        failedTransformations = new HashMap<>();
    }

    String[] rules;
    boolean remove = true;
    private boolean modifiedTree;
    private boolean detmode;
    private int counter;
    private String currentStmt;

    public AssignmentRemover(String testfile, MyLogger logger) {
        super(TMUtil.getTemplateParser(testfile), logger);
        rules = this.parser.getRuleNames();
        print("Running assignment remover");
    }

    public AssignmentRemover(Template2Parser parser, boolean detmode, int counter, MyLogger logger) {
        this(parser, logger);
        this.detmode = detmode;
        this.counter = counter;
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, this.parser.template());
    }

    public AssignmentRemover(Template2Parser parser, MyLogger logger) {
        super(parser, logger);
    }

    public static String Transform(String testfile) {
        AssignmentRemover assignmentRemover = new AssignmentRemover(testfile, null);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(assignmentRemover, assignmentRemover.parser.template());
        return assignmentRemover.rewriter.getText();
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                if (!AssignmentRemover.failedTransformations.containsKey(testfile)) {
                    AssignmentRemover.failedTransformations.put(testfile, new ArrayList<>());
                }

                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int assign_stmt = TemplateCounter.getCount(testfile, "assign");
                logger.print("Assign statements: " + assign_stmt, true);
                for (int l = 0; l < assign_stmt; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    AssignmentRemover assignmentRemover = new AssignmentRemover(parser, logger);
                    assignmentRemover.counter = l + 1;
                    assignmentRemover.detmode = true;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(assignmentRemover, assignmentRemover.parser.template());
                    String newContent = assignmentRemover.rewriter.getText();
                    if (assignmentRemover.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(AssignmentRemover.class),
                                    logger,
                                    testFile)) {
                        filecontent = newContent;
                        changed = true;
                        logger.print("Changed", true);
                    } else {
                        l++;
                        ArrayList<String> fails = AssignmentRemover.failedTransformations.get(testfile);
                        if (fails.contains(assignmentRemover.currentStmt)) {
                            logger.print("Failed again: " + assignmentRemover.currentStmt, true);
                        } else if (assignmentRemover.currentStmt != null) {
                            fails.add(assignmentRemover.currentStmt);
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

    public boolean isModified() {
        return this.modifiedTree;
    }

    public String getText() {
        return this.rewriter.getText();
    }

    @Override
    public void exitAssign(Template2Parser.AssignContext ctx) {
        ParserRuleContext parent = ctx.getParent().getParent();
        if (detmode) {
            counter--;
            if (counter == 0) {
                currentStmt = ctx.getText();
                print("Removing statement : " + ctx.start.getInputStream().getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex())));
                if (parent.getRuleIndex() == Template2Parser.RULE_block && parent.children.size() == 1) {
                    this.rewriter.replace(ctx.getStart(), ctx.getStop(), "{ }");
                } else {
                    this.rewriter.delete(ctx.getStart(), ctx.getStop());
                }
                modifiedTree = true;
            }
        }
    }


}

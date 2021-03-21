package tool.testmin.templatetransformers;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringUtils;
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
import java.util.List;
import java.util.ListIterator;

public class LoopEliminator extends TemplateBaseTransformer {
    int counter = 0;
    boolean checkPriors;
    private boolean modifiedTree;

    public LoopEliminator(Template2Parser parser, MyLogger logger, boolean checkPriors) {
        super(parser, logger);
        this.checkPriors = checkPriors;
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int loops = TemplateCounter.getCount(testfile, "for_loop_stmt");
                for (int l = 0; l < loops; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    LoopEliminator loopEliminator = new LoopEliminator(parser, logger, false);
                    loopEliminator.counter = l + 1;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(loopEliminator, loopEliminator.parser.template());
                    String newContent = loopEliminator.rewriter.getText();
                    if (loopEliminator.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(LoopEliminator.class),
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
            System.out.println("Replacing loop:: ");
            System.out.println(ctx.getText());
            if (checkPriors) {
                String priors = getPriors(ctx.block().statement());

                if (priors.length() > 0) {
                    System.out.println("Replaced priors::");
                    System.out.println(priors);
                } else {
                    System.out.println("No replaced priors");
                }

                this.rewriter.replace(ctx.getStart(), ctx.getStop(), priors);
            } else {
                this.rewriter.delete(ctx.getStart(), ctx.getStop());
            }
            this.modifiedTree = true;
            print("Eliminiated loop...");
        }
    }

    private String getPriors(List<Template2Parser.StatementContext> statements) {
        ArrayList<Template2Parser.StatementContext> worklist = new ArrayList<>(statements);
        String dummyPriors = "";
        while (worklist.size() > 0) {
            List<Template2Parser.StatementContext> toremove = new ArrayList<>(worklist);

            for (ListIterator<Template2Parser.StatementContext> iterator = worklist.listIterator(); iterator.hasNext(); ) {
                Template2Parser.StatementContext statementContext = iterator.next();
                if (statementContext.prior() != null) {
                    dummyPriors += String.format("%s := normal%s(%s)%s\n",
                            TMUtil.getParamName(statementContext.prior()),
                            statementContext.prior().distexpr().vectorDIMS() != null ? statementContext.prior().distexpr().vectorDIMS().getText() : "",
                            StringUtils.repeat("1234.0", ",", statementContext.prior().distexpr().params().param().size()),
                            statementContext.prior().distexpr().dims() != null ? "[" + statementContext.prior().distexpr().dims().getText() + "]" : "");
                } else if (statementContext.for_loop() != null) {
                    statementContext.for_loop().block().statement().forEach(x -> iterator.add(x));
                } else if (statementContext.if_stmt() != null) {
                    statementContext.if_stmt().block().statement().forEach(x -> iterator.add(x));
                    if (statementContext.if_stmt().else_blk() != null) {
                        statementContext.if_stmt().else_blk().block().statement().forEach(x -> iterator.add(x));
                    }
                }
            }

            worklist.removeAll(toremove);
        }
        return dummyPriors;
    }


}

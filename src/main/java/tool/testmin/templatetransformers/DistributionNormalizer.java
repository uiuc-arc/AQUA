package tool.testmin.templatetransformers;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;
import tool.testmin.util.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DistributionNormalizer extends TemplateBaseTransformer {
    private int ruleIndex = 0;
    private boolean modifiedTree;
    private String filename;
    private String replacedCtx;
    private String pps;
    private String program;


    public DistributionNormalizer(Template2Parser parser, MyLogger logger, String filename, String pps) {
        super(parser, logger);
        this.filename = filename;
        this.pps = pps;
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int dists = TemplateCounter.getCount(testfile, "distribution_exp");
                logger.print("Distributions: " + dists, true);
                for (int l = 0; l < dists; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    DistributionNormalizer distributionNormalizer = new DistributionNormalizer(parser, logger, testfile, testFile.getPps());
                    distributionNormalizer.ruleIndex = l + 1;
                    distributionNormalizer.program = filecontent;

                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(distributionNormalizer, distributionNormalizer.parser.template());
                    String newContent = distributionNormalizer.rewriter.getText();
                    if (distributionNormalizer.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(DistributionNormalizer.class),
                                    logger,
                                    testFile)) {
                        filecontent = newContent;
                        changed = true;
                        logger.print("Changed", true);
                    } else {
                        TMUtil.removeReplacedDist(testfile, distributionNormalizer.replacedCtx);
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
    public void exitDistexpr(Template2Parser.DistexprContext ctx) {
        ruleIndex--;

        if (ruleIndex == 0) {
            try {
                if (ctx.getText().contains("1234.0")) {
                    print("Dummy dist expr.. skipping " + ctx.getText());

                    return;
                } else if (TMUtil.checkIfReplaced(this.filename, ctx)) {
                    print("Already replaced distribution... skipping... " + ctx.getText());
                    return;
                }

                print("Removing " + ctx.ID().getText() + " dist: " + TMUtil.getStringFromContext(ctx) + " at line : " + ctx.getStart().getLine());
                DistributionChooser distributionChooser = new DistributionChooser(this.logger);
                String newDist = distributionChooser.getSupportingDistrib2(program, ctx.ID().getText(), ctx);

                String newlimit = updateLimit(ctx, distributionChooser.getChosenModelSupport());
                if (newDist != null && !newDist.isEmpty()) {
                    rewriter.replace(ctx.getStart(), ctx.getStop(), newDist + newlimit);
                    modifiedTree = true;
                    this.replacedCtx = newDist.trim();
                    print(String.format("Replacing distribution %s with %s", ctx.getText(), newDist));
                } else {
                    print("No substitute distribution found");
                }
            } catch (Exception e) {
                print("Distribution Substitution failed");
                e.printStackTrace();
            }
        }
    }

    private String updateLimit(Template2Parser.DistexprContext ctx, String chosenModelSupport) {
        if (ctx != null && chosenModelSupport != null && chosenModelSupport.contains("+") && ctx.getParent() instanceof Template2Parser.PriorContext) {
            Template2Parser.PriorContext priorContext = (Template2Parser.PriorContext) ctx.getParent();
            if (priorContext.limits() != null) {
                if (priorContext.limits().getText().contains("lower")) {
                    print("Already has limits..." + priorContext.getText());
                    return "";
                }
            } else {
                print("Adding 0 limit");
                return "<lower=0>";
            }
        }

        return "";
    }
}

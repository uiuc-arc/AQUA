package tool.testmin.templatetransformers;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import grammar.Template2Parser;
import tool.testmin.TemplateBaseTransformer;
import tool.testmin.util.MyLogger;
import tool.testmin.util.TemplateCounter;
import tool.testmin.util.TestFile;
import tool.testmin.util.TMUtil;
import tool.testmin.util.template.TemplateDimensionChecker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ArithEliminator extends TemplateBaseTransformer {
    private char op;
    private int count = -1;
    private boolean modifiedTree;
    private boolean detmode = false;
    private String program;
    private boolean first;
    private boolean analyse;
    private boolean inFunctions;

    public ArithEliminator(Template2Parser parser, char op, MyLogger logger, boolean first) {
        this(parser, op, logger, first, false);
    }

    public ArithEliminator(Template2Parser parser, char op, MyLogger logger, boolean first, boolean analyse) {
        super(parser, logger);
        this.op = op;
        print("Running ArithEliminator : " + this.op);
        this.first = first;
        this.analyse = analyse;
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        logger.print(testfile + "......", true);
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int adds = TemplateCounter.getCount(testfile, "addop");
                int subs = TemplateCounter.getCount(testfile, "minusop");
                int divs = TemplateCounter.getCount(testfile, "divop");
                int muls = TemplateCounter.getCount(testfile, "mulop");
                int expons = TemplateCounter.getCount(testfile, "exponop");
                logger.print("adds:" + adds, true);
                logger.print("subs:" + subs, true);
                logger.print("muls:" + muls, true);
                logger.print("divs:" + divs, true);
                logger.print("expons:" + expons, true);

                boolean add_first = true;

                for (int l = 0; l < adds; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    ArithEliminator arithEliminator = new ArithEliminator(parser, '+', logger, add_first);

                    arithEliminator.count = l + 1;
                    arithEliminator.detmode = true;
                    arithEliminator.program = filecontent;

                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(arithEliminator, arithEliminator.parser.template());
                    String newContent = arithEliminator.rewriter.getText();
                    if (arithEliminator.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(ArithEliminator.class),
                                    logger,
                                    testFile)) {
                        filecontent = newContent;
                        changed = true;
                        logger.print("Changed", true);
                    } else {
                        if (add_first)
                            add_first = false;
                        else {
                            add_first = true;
                            l++;
                        }

                    }
                }

                boolean sub_first = true;
                for (int l = 0; l < subs; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    ArithEliminator arithEliminator = new ArithEliminator(parser, '-', logger, sub_first);

                    arithEliminator.count = l + 1;
                    arithEliminator.detmode = true;
                    arithEliminator.program = filecontent;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(arithEliminator, arithEliminator.parser.template());
                    String newContent = arithEliminator.rewriter.getText();
                    if (arithEliminator.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(ArithEliminator.class),
                                    logger,
                                    testFile)) {
                        filecontent = newContent;
                        changed = true;
                        logger.print("Changed", true);
                    } else {
                        if (sub_first) {
                            sub_first = false;
                        } else {
                            sub_first = true;
                            l++;
                        }
                    }
                }


                for (int l = 0; l < muls; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    ArithEliminator arithEliminator = new ArithEliminator(parser, '*', logger, false);

                    arithEliminator.count = l + 1;
                    arithEliminator.detmode = true;
                    arithEliminator.program = filecontent;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(arithEliminator, arithEliminator.parser.template());
                    String newContent = arithEliminator.rewriter.getText();
                    if (arithEliminator.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(ArithEliminator.class),
                                    logger,
                                    testFile)) {

                        filecontent = newContent;
                        changed = true;
                        logger.print("Changed", true);
                    } else {

                        l++;
                    }
                }


                for (int l = 0; l < divs; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    ArithEliminator arithEliminator = new ArithEliminator(parser, '/', logger, false);

                    arithEliminator.count = l + 1;
                    arithEliminator.detmode = true;
                    arithEliminator.program = filecontent;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(arithEliminator, arithEliminator.parser.template());
                    String newContent = arithEliminator.rewriter.getText();
                    if (arithEliminator.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(ArithEliminator.class),
                                    logger,
                                    testFile)) {

                        filecontent = newContent;
                        changed = true;
                        logger.print("Changed", true);
                    } else {

                        l++;
                    }
                }


                for (int l = 0; l < expons; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    ArithEliminator arithEliminator = new ArithEliminator(parser, '^', logger, false);

                    arithEliminator.count = l + 1;
                    arithEliminator.detmode = true;
                    arithEliminator.program = filecontent;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(arithEliminator, arithEliminator.parser.template());
                    String newContent = arithEliminator.rewriter.getText();
                    if (arithEliminator.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(ArithEliminator.class),
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
    public void enterFunctions(Template2Parser.FunctionsContext ctx) {
        this.inFunctions = true;
    }

    @Override
    public void exitFunctions(Template2Parser.FunctionsContext ctx) {
        this.inFunctions = false;
    }

    @Override
    public void exitAddop(Template2Parser.AddopContext ctx) {
        if (this.detmode) {
            if (this.op == '+') {
                this.count--;
                if (this.count == 0) {
                    if (this.analyse) {
                        // if one is primitive, another is not, choose the other one
                        boolean isPrimitive0 = TMUtil.isPrimitive(ctx.expr(0), program);
                        boolean isPrimitive1 = TMUtil.isPrimitive(ctx.expr(1), program);
                        if (isPrimitive0 && !isPrimitive1) {
                            rewriter.replace(ctx.getStart(), ctx.getStop(), ctx.expr(1).getText());
                            print("Reduced Add op, 2nd operand non-primitive");
                            this.modifiedTree = true;
                            return;
                        } else if (!isPrimitive0 && isPrimitive1) {
                            rewriter.replace(ctx.getStart(), ctx.getStop(), ctx.expr(0).getText());
                            print("Reduced Add op, 1st operand non-primitive");
                            this.modifiedTree = true;
                            return;
                        }
                    }

                    if (!first) {
                        rewriter.replace(ctx.getStart(), ctx.getStop(), ctx.expr(1).getText());
                    } else {
                        rewriter.replace(ctx.getStart(), ctx.getStop(), ctx.expr(0).getText());
                    }
                    print("Reduced Add op");
                    this.modifiedTree = true;
                }
            }
        }
    }

    public void exitMinusop(Template2Parser.MinusopContext ctx) {
        if (this.detmode) {
            if (this.op == '-') {
                this.count--;
                if (this.count == 0) {
                    if (this.analyse) {
                        boolean isPrimitive0 = TMUtil.isPrimitive(ctx.expr(0), program);
                        boolean isPrimitive1 = TMUtil.isPrimitive(ctx.expr(1), program);
                        if (isPrimitive0 && !isPrimitive1) {
                            rewriter.replace(ctx.getStart(), ctx.getStop(), ctx.expr(1).getText());
                            print("Reduced minus op, 2nd operand non-primitive");
                            this.modifiedTree = true;
                            return;
                        } else if (!isPrimitive0 && isPrimitive1) {
                            rewriter.replace(ctx.getStart(), ctx.getStop(), ctx.expr(0).getText());
                            print("Reduced minus op, 1st operand non-primitive");
                            this.modifiedTree = true;
                            return;
                        }
                    }
                    if (!first) {
                        rewriter.replace(ctx.getStart(), ctx.getStop(), ctx.expr(1).getText());
                    } else {
                        rewriter.replace(ctx.getStart(), ctx.getStop(), ctx.expr(0).getText());
                    }
                    print("Reduced minus op");
                    this.modifiedTree = true;
                }
            }
        }
    }

    @Override
    public void exitMulop(Template2Parser.MulopContext ctx) {
        if (this.detmode) {
            if (this.op == '*') {
                this.count--;
                if (this.count == 0) {
                    String replacement = TemplateDimensionChecker.getSubstitute(program, ctx);
                    if (this.inFunctions && replacement.contains("rep_")) {
                        Matcher matcher = Pattern.compile("[a-zA-Z_]+").matcher(replacement.replaceFirst("rep_[^(]*", ""));
                        if (matcher.find()) {
                            print("Skipping replacement: " + replacement);
                            return;
                        }
                    }

                    if (replacement != null) {
                        print("Replacement " + replacement);
                        rewriter.replace(ctx.getStart(), ctx.getStop(), replacement);
                        print("Reduced mul op");
                        this.modifiedTree = true;
                    } else {
                        print("Replacement not found for: " + ctx.getText());
                    }
                }
            }
        }
    }

    @Override
    public void exitDivop(Template2Parser.DivopContext ctx) {
        if (this.detmode) {
            if (this.op == '/') {
                this.count--;
                if (this.count == 0) {
                    String replacement = TemplateDimensionChecker.getSubstitute(program, ctx);
                    if (this.inFunctions && replacement.contains("rep_")) {
                        Matcher matcher = Pattern.compile("[a-zA-Z_]+").matcher(replacement.replaceFirst("rep_[^(]*", ""));
                        if (matcher.find()) {
                            print("Skipping replacement: " + replacement);
                            return;
                        }
                    }
                    if (replacement != null) {
                        print("Replacement " + replacement);
                        rewriter.replace(ctx.getStart(), ctx.getStop(), replacement);
                        print("Reduced div op");
                        this.modifiedTree = true;
                    } else {
                        print("Replacement not found for: " + ctx.getText());
                    }
                }
            }
        }
    }

    @Override
    public void exitExponop(Template2Parser.ExponopContext ctx) {
        if (this.detmode) {
            if (this.op == '^') {
                this.count--;
                if (this.count == 0) {
                    rewriter.replace(ctx.getStart(), ctx.getStop(), ctx.expr(0).getText());
                    print("Reduced expon op");
                    this.modifiedTree = true;
                }
            }
        }
    }

}

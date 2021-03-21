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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunctionCallEliminator extends TemplateBaseTransformer {
    private static ArrayList<String> realStructureFunctions = new ArrayList<>(Arrays.asList("log", "sqrt", "sqrt2", "softmax", "multi_normal_rng"));
    private static ArrayList<String> realfunctions = new ArrayList<>(Arrays.asList("log_sum_exp", "normal_cdf_log", "normal_lpdf", "mean", "sd", "gamma_p", "normal_rng",
            ""));

    private static ArrayList<String> intfunctions = new ArrayList<>(Arrays.asList("binomial_rng", "bernoulli_logit_rng"));
    private static ArrayList<String> skipFunctions = new ArrayList<>(Arrays.asList("increment_log_prob", "cov_exp_quad", "return"));
    private boolean replace = true;
    private boolean modifiedTree;
    private int counter = 0;
    private boolean detMode;
    private String program;
    private boolean inFunctions;

    public FunctionCallEliminator(Template2Parser parser, MyLogger logger) {
        super(parser, logger);
    }

    public FunctionCallEliminator(String file, MyLogger logger) {
        super(TMUtil.getTemplateParser(file), logger);
        print("Running function call eliminator");
    }

    public static String Transform(String testfile) {
        FunctionCallEliminator functionCallEliminator = new FunctionCallEliminator(testfile, null);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(functionCallEliminator, functionCallEliminator.parser.template());
        return functionCallEliminator.rewriter.getText();
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int function_calls = TemplateCounter.getCount(testfile, "function_call");
                logger.print("function calls: " + function_calls, true);
                for (int l = 0; l < function_calls; ) {
                    Template2Parser parser = TMUtil.getTemplateParserFromString(filecontent);
                    FunctionCallEliminator functionCallEliminator = new FunctionCallEliminator(parser, logger);
                    functionCallEliminator.counter = l + 1;
                    functionCallEliminator.detMode = true;
                    functionCallEliminator.program = filecontent;
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(functionCallEliminator, functionCallEliminator.parser.template());
                    String newContent = functionCallEliminator.rewriter.getText();
                    if (functionCallEliminator.modifiedTree &&
                            TMUtil.runTransformedCode(filecontent,
                                    newContent,
                                    TMUtil.TemplateTransformationMap.inverse().get(FunctionCallEliminator.class),
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
        inFunctions = true;
    }

    @Override
    public void exitFunctions(Template2Parser.FunctionsContext ctx) {
        inFunctions = false;
    }

    @Override
    public void exitFunction(Template2Parser.FunctionContext ctx) {
        String funcName = ctx.function_call().FUNCTION().getText();
        if (detMode) {
            counter--;
            if (counter == 0) {
                if (realfunctions.indexOf(funcName) >= 0) {
                    print("Rewriting:" + funcName);
                    this.rewriter.replace(ctx.getStart(), ctx.getStop(), "0.5");
                    this.modifiedTree = true;
                } else if (realStructureFunctions.indexOf(funcName) >= 0) {
                    String replacement = TemplateDimensionChecker.getSubstitute(program, ctx.function_call().params().param(0), "0.5");

                    if (this.inFunctions && replacement.contains("rep_")) {
                        Matcher matcher = Pattern.compile("[a-zA-Z_]+").matcher(replacement.replaceFirst("rep_[^(]*", ""));
                        if (matcher.find()) {
                            print("Skipping replacement: " + replacement);
                            return;
                        }
                    }

                    if (replacement != null) {
                        print("Rewriting:" + funcName);
                        this.rewriter.replace(ctx.getStart(), ctx.getStop(), replacement);
                        this.modifiedTree = true;
                    } else {
                        print("Missing function: " + funcName);
                    }
                } else if (intfunctions.indexOf(funcName) >= 0) {
                    String replacement = "1";

                    if (replacement != null) {
                        print("Rewriting:" + funcName);
                        this.rewriter.replace(ctx.getStart(), ctx.getStop(), replacement);
                        this.modifiedTree = true;
                    } else {
                        print("Missing function:" + funcName);
                    }
                } else if (skipFunctions.indexOf(funcName) >= 0) {
                    print("Skipping function: " + funcName);
                } else {
                    String replacement = TemplateDimensionChecker.getSubstitute(program, ctx);
                    if (this.inFunctions && replacement.contains("rep_")) {
                        Matcher matcher = Pattern.compile("[a-zA-Z_]+").matcher(replacement.replaceFirst("rep_[^(]*", ""));
                        if (matcher.find()) {
                            print("Skipping replacement: " + replacement);
                            return;
                        }
                    }
                    System.out.println("Replacement: " + replacement);
                    if (replacement != null) {
                        this.rewriter.replace(ctx.getStart(), ctx.getStop(), replacement);
                        this.modifiedTree = true;
                        print("Replaced function using dimension checker");
                    } else {
                        print("Missing function:" + funcName);
                    }
                }
            }
        }
    }


}

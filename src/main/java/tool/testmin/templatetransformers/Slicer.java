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
import java.util.*;

public class Slicer extends TemplateBaseTransformer {
    private Map<String, Integer> defuse;

    private String currentAssignId;
    private Map<String, Integer> sampleUse;
    private Map<String, Integer> observeUse;

    private String currentSampleId;
    private String currentObserveId;
    private int counterAssign;
    private int counterSample;
    private int counterObserve;
    private Set<Integer> UnexpectedAssign;
    private Set<Integer> UnexpectedSample;

    public Slicer(Template2Parser parser, MyLogger logger) {
        super(parser, logger);
        this.defuse = new HashMap<>();
        this.sampleUse = new HashMap<>();
        this.observeUse = new HashMap<>();
        this.UnexpectedAssign = new HashSet<>();
        this.UnexpectedSample = new HashSet<>();
        counterAssign = 0;
        counterSample = 0;
        counterObserve = 0;
    }

    public static boolean Transform(String testfile, boolean detmode, MyLogger logger, String testdir, TestFile testFile) {
        boolean changed = false;
        int successful_assigns = 0;
        int successful_samples = 0;
        int successful_observes = 0;
        Set<Integer> assignsToRemove = new HashSet<Integer>();
        Set<Integer> samplesToRemove = new HashSet<Integer>();
        Set<Integer> observesToRemove = new HashSet<>();
        if (detmode) {
            try {
                logger.print("Running Slicer ", true);
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int assign_stmt = TemplateCounter.getCount(testfile, "assign");
                logger.print("Assign statements: " + assign_stmt, true);

                for (int l = assign_stmt; l > 0; ) {

                    Slicer slicer = new Slicer(TMUtil.getTemplateParserFromString(filecontent), logger);
                    new ParseTreeWalker().walk(slicer, slicer.parser.template());
                    Collection<Integer> assignToRemove = slicer.defuse.values();

                    assignsToRemove.addAll(assignToRemove);
                    assignsToRemove.addAll(slicer.UnexpectedAssign);


                    if (assignToRemove.contains(l) || slicer.UnexpectedAssign.contains(l)) {
                        AssignmentRemover assignmentRemover = new AssignmentRemover(
                                TMUtil.getTemplateParserFromString(filecontent),
                                true,
                                l, logger);

                        String newContent = assignmentRemover.getText();
                        if (assignmentRemover.isModified() &&
                                TMUtil.runTransformedCode(filecontent,
                                        newContent,
                                        TMUtil.TemplateTransformationMap.inverse().get(Slicer.class),
                                        logger,
                                        testFile)) {

                            filecontent = newContent;
                            changed = true;

                            assignsToRemove.remove(l);
                            successful_assigns++;

                            logger.print("Changed", true);
                            l = TemplateCounter.getCount(testfile, "assign");
                        } else {

                            l--;
                        }
                    } else {
                        l--;
                    }
                }

                logger.print("Successful assigns : " + successful_assigns, true);
                logger.print("Total assign candidates : " + assignsToRemove.size(), true);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int sample = TemplateCounter.getCount(testfile, "prior");
                logger.print("Sample statements: " + sample, true);
                for (int l = sample; l > 0; ) {

                    Slicer slicer = new Slicer(TMUtil.getTemplateParserFromString(filecontent), logger);
                    new ParseTreeWalker().walk(slicer, slicer.parser.template());
                    Collection<Integer> sampleToRemove = slicer.sampleUse.values();

                    samplesToRemove.addAll(samplesToRemove);
                    samplesToRemove.addAll(slicer.UnexpectedSample);


                    if (sampleToRemove.contains(l) || slicer.UnexpectedSample.contains(l)) {
                        SamplingRemover samplingRemover = new SamplingRemover(
                                TMUtil.getTemplateParserFromString(filecontent),
                                true,
                                l, logger);

                        String newContent = samplingRemover.getText();
                        if (samplingRemover.isModified() &&
                                TMUtil.runTransformedCode(filecontent,
                                        newContent,
                                        TMUtil.TemplateTransformationMap.inverse().get(Slicer.class),
                                        logger,
                                        testFile)) {

                            filecontent = newContent;
                            changed = true;

                            successful_samples++;
                            samplesToRemove.remove(l);

                            logger.print("Changed", true);
                            l = TemplateCounter.getCount(testfile, "prior");

                        } else {

                            l--;
                        }
                    } else {
                        l--;
                    }
                }

                logger.print("Successful samples : " + successful_samples, true);
                logger.print("Candidates to remove: " + samplesToRemove.size(), true);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (detmode) {
            try {
                String filecontent = new String(Files.readAllBytes(Paths.get(testfile)));
                int observe = TemplateCounter.getCount(testfile, "observe");
                logger.print("Observe statements: " + observe, true);
                for (int l = observe; l > 0; ) {

                    Slicer slicer = new Slicer(TMUtil.getTemplateParserFromString(filecontent), logger);
                    new ParseTreeWalker().walk(slicer, slicer.parser.template());
                    Collection<Integer> observeToRemove = slicer.observeUse.values();

                    observesToRemove.addAll(observeToRemove);


                    if (observeToRemove.contains(l)) {
                        ObserveRemover observeRemover = new ObserveRemover(TMUtil.getTemplateParserFromString(filecontent),
                                true,
                                l,
                                logger);


                        String newContent = observeRemover.getText();
                        if (observeRemover.isModified() &&
                                TMUtil.runTransformedCode(filecontent,
                                        newContent,
                                        TMUtil.TemplateTransformationMap.inverse().get(Slicer.class),
                                        logger,
                                        testFile)) {

                            filecontent = newContent;
                            changed = true;

                            successful_observes++;
                            observesToRemove.remove(l);

                            logger.print("Changed", true);
                            l = TemplateCounter.getCount(testfile, "prior");

                        } else {

                            l--;
                        }
                    } else {
                        l--;
                    }
                }

                logger.print("Successful observes : " + successful_observes, true);
                logger.print("Candidates to remove: " + observesToRemove.size(), true);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return changed;
    }

    @Override
    public void enterAssign(Template2Parser.AssignContext ctx) {
        counterAssign++;
        if (ctx.ID() != null) {
            this.currentAssignId = ctx.ID().getText();

        } else if (ctx.expr(0) instanceof Template2Parser.RefContext) {
            this.currentAssignId = ((Template2Parser.RefContext) ctx.expr(0)).ID().getText();
        } else if (ctx.expr(0) instanceof Template2Parser.Array_accessContext) {
            this.currentAssignId = ((Template2Parser.Array_accessContext) ctx.expr(0)).ID().getText();
        } else {
            print("Unexpected assign: " + ctx.getText());
            this.currentAssignId = null;
            return;
        }
        this.defuse.put(this.currentAssignId, counterAssign);
    }

    @Override
    public void exitAssign(Template2Parser.AssignContext ctx) {
        this.currentAssignId = null;
    }

    @Override
    public void enterObserve(Template2Parser.ObserveContext ctx) {
        counterObserve++;
        int index = 0;
        if (ctx.expr().size() > 1) {
            index = 1;
        }
        if (ctx.expr().get(index) instanceof Template2Parser.Array_accessContext) {
            this.currentObserveId = ((Template2Parser.Array_accessContext) ctx.expr().get(index)).ID().getText();
        } else if (ctx.expr(index) instanceof Template2Parser.RefContext) {
            this.currentObserveId = ((Template2Parser.RefContext) ctx.expr(index)).ID().getText();
        } else {
            print("Unexpected observe" + ctx.getText());
            return;
        }

        this.observeUse.put(this.currentObserveId, counterObserve);
    }

    @Override
    public void exitObserve(Template2Parser.ObserveContext ctx) {
        this.currentObserveId = null;
    }

    @Override
    public void enterRef(Template2Parser.RefContext ctx) {
        if (!ctx.ID().getText().equals(this.currentAssignId)) {
            this.defuse.remove(ctx.ID().getText());
        }

        if (!ctx.ID().getText().equals(this.currentSampleId)) {
            this.sampleUse.remove(ctx.ID().getText());

        }


    }

    @Override
    public void enterPrior(Template2Parser.PriorContext ctx) {
        counterSample++;
        if (ctx.getText().contains("1234.0"))

            return;

        if (ctx.expr() instanceof Template2Parser.RefContext) {
            this.currentSampleId = ((Template2Parser.RefContext) ctx.expr()).ID().getText();
        } else if (ctx.expr() instanceof Template2Parser.Array_accessContext) {
            this.currentSampleId = ((Template2Parser.Array_accessContext) ctx.expr()).ID().getText();
        } else {
            print("Unexpected assign: " + ctx.getText());
        }

        this.sampleUse.put(this.currentSampleId, counterSample);
    }

    @Override
    public void exitPrior(Template2Parser.PriorContext ctx) {
        this.currentSampleId = null;
    }

    public void walk() {
        new ParseTreeWalker().walk(this, this.parser.template());
    }

    public ArrayList<Integer> getDefUse() {
        ArrayList<Integer> vals = new ArrayList<>(this.defuse.values());
        vals.addAll(this.UnexpectedAssign);
        return vals;
    }

    public ArrayList<Integer> getSampleUse() {
        ArrayList<Integer> vals = new ArrayList<>(this.sampleUse.values());
        vals.addAll(this.UnexpectedSample);
        return vals;
    }

    public ArrayList<Integer> getObserveUse() {
        ArrayList<Integer> vals = new ArrayList<>(this.observeUse.values());
        return vals;
    }


}

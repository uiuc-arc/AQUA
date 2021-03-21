package tool.testmin;

import tool.testmin.templatetransformers.DistributionNormalizer;
import tool.testmin.util.MyLogger;
import tool.testmin.util.TestFile;
import tool.testmin.util.TMUtil;

import java.util.Date;

public class TestMinimizer {
    public static boolean enableAnalysis;
    public static String pps;
    private String testfile;
    private String testdir;
    private MyLogger logger;
    private String datafile;
    private TestFile testFile;

    public TestMinimizer(TestFile fileinfo) {
        testfile = fileinfo.getTestfile();
        testdir = fileinfo.getTestdir();
        testFile = fileinfo;
        pps = fileinfo.getPps();

        try {
            if (fileinfo.getDatafile() != null) {
                datafile = fileinfo.getDatafile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TestMinimizer(TestFile fileinfo, MyLogger logger) {
        this(fileinfo);
        this.logger = logger;
    }

    public void template_minimize_greedy_det(boolean onlyBasic, boolean enableAnalysis) {
        Date start = new Date(System.currentTimeMillis());
        TestMinimizer.enableAnalysis = enableAnalysis;
        int iterations = 1;
        boolean gchanged = true;
        while (gchanged) {
            gchanged = false;
            logger.print("\n===================================== Starting iteration : " + iterations + " ===================================== ", true);
            for (int t = 0; t <= 15; t++) {
                boolean changed = false;
                int transformation = TMUtil.order.get(t);
                if (onlyBasic && TMUtil.isDomain(transformation)) {
                    logger.print("Skipping domain ...", true);
                    continue;
                }
                if (TMUtil.RunCov) {

                    if (transformation == 9 || transformation == 12)
                        continue;
                }

                if (!TMUtil.TemplateTransformationMap.containsKey(transformation))
                    continue;

                logger.print("Running transformer: " + TMUtil.TemplateTransformationMap.get(transformation).getCanonicalName(), true);
                switch (transformation) {
                    case 0:
                        changed = tool.testmin.templatetransformers.LimitsRemover.Transform(testfile, true, logger, testdir, testFile);
                        break;
                    case 1:
                        changed = tool.testmin.templatetransformers.LoopVarRemover.Transform(testfile, true, logger, testdir, testFile);
                        break;
                    case 2:
                        changed = tool.testmin.templatetransformers.ArithEliminator.Transform(testfile, true, logger, testdir, testFile);
                        break;
                    case 3:
                        changed = tool.testmin.templatetransformers.FunctionCallEliminator.Transform(testfile, true, logger, testdir, testFile);
                        break;
                    case 4:
                        changed = tool.testmin.templatetransformers.FunctionCallStmtEliminator.Transform(testfile, true, logger, testdir, testFile);
                        break;
                    case 5:
                        changed = tool.testmin.templatetransformers.LoopEliminator.Transform(testfile, true, logger, testdir, testFile);
                        break;
                    case 6:
                        changed = tool.testmin.templatetransformers.ConditionalEliminator.Transform(testfile, true, logger, testdir, testFile);
                        break;


                    case 9:
                        changed = DistributionNormalizer.Transform(testfile, true, logger, testdir, testFile);
                        break;
                    case 10:
                        changed = tool.testmin.templatetransformers.ParameterEliminator.Transform(testfile, true, logger, testdir, testFile);
                        break;
                    case 11:
                        changed = tool.testmin.templatetransformers.DataTransformer.Transform(testfile, true, logger, testdir, testFile);
                        break;
                    case 12:
                        changed = tool.testmin.templatetransformers.SamplerReducer.Transform(testfile, true, logger, testdir, testFile);
                        break;


                    case 14:
                        logger.print("Removing unused parts ....", true);
                        changed = tool.testmin.templatetransformers.UnusedItemRemover.Transform(testfile, datafile, logger, testdir, testFile);
                        break;
                    case 15:
                        changed = tool.testmin.templatetransformers.Slicer.Transform(testfile, true, logger, testdir, testFile);
                        break;
                }
                if (changed)
                    gchanged = true;
            }
            iterations++;
        }

        logger.print("Iterations : " + iterations, true);

        Date end = new Date(System.currentTimeMillis());
        long timediff = end.getTime() - start.getTime();
        logger.print("Time for " + testfile + " : " + timediff, true);
        TMUtil.TimingMap.put(testfile, timediff);

    }

}

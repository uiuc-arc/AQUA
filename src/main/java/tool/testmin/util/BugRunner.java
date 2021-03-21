package tool.testmin.util;

import tool.testmin.TestMinimizer;

import java.io.StringWriter;

public class BugRunner implements Runnable {
    private TestFile bugFile;
    private StringWriter stringWriter;
    private String output;
    private MyLogger logger;
    private String threadName;

    public BugRunner(TestFile bugfile, String threadName, String dir_suffix) {
        bugFile = bugfile;
        logger = new MyLogger(bugfile, threadName, dir_suffix);
        this.threadName = threadName;
    }

    @Override
    public void run() {

        logger.print("++++++++++++++++++++++++++++++++ Starting thread for program : " + this.threadName + " ++++++++++++++++++++++++++++++++", true);

        new TestMinimizer(bugFile, logger).template_minimize_greedy_det(TMUtil.RUNBASICONLY, true);
        logger.print("++++++++++++++++++++++++++++++++ Closing thread for program : " + this.threadName + " ++++++++++++++++++++++++++++++++", true);
    }
}

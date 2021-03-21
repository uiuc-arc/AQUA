package tool.testmin;

import tool.testmin.util.BugRunner;
import tool.testmin.util.MyThreadFactory;
import tool.testmin.util.TestFile;
import tool.testmin.util.TMUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class TMMain {

    public static String curFile;

    public static void main(String[] args) {
        ArrayList<TestFile> testfiles = TMUtil.getTestFiles();

        String dir_suffix;
        if (args.length > 0) {
            dir_suffix = args[0];
        } else {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            dir_suffix = Long.toString(timestamp.getSeconds() + 60);
        }

        System.out.println("Output dir suffix : " + dir_suffix);

        ExecutorService threadpoolservice = Executors.newFixedThreadPool(TMUtil.MAX_THREADS, new MyThreadFactory());
        Date start = new Date(System.currentTimeMillis());

        for (TestFile fileinfo : testfiles) {
            String name = fileinfo.getFilename();
            curFile = name;
            threadpoolservice.execute(new BugRunner(fileinfo, name, dir_suffix));
        }

        threadpoolservice.shutdown();

        try {
            threadpoolservice.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Date end = new Date(System.currentTimeMillis());

        TMUtil.createReport((end.getTime() - start.getTime()), dir_suffix);
        TMUtil.restoreTestFiles();
        System.out.println("Output directory : diag_" + dir_suffix);
    }
}

package grammar.analyses;

import grammar.cfg.CFGBuilder;
import grammar.cfg.Section;
import org.apache.commons.io.FileUtils;
import translators.Stan2IRTranslator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class AnalysisRunner {

    public static void analyzeProgram (String localDir, String stanName) {
        String stanfile = localDir + stanName + "/" + stanName + ".stan";
        String standata = localDir + stanName + "/" + stanName + ".data.R";
        Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator(stanfile, standata);
        String tempFileName = stanfile.replace(".stan", "");
        String templateCode = stan2IRTranslator.getCode();
        System.out.println("========Stan Code to Template=======");
        System.out.println(templateCode);
        File tempfile = null;
        try {
            tempfile = File.createTempFile(tempFileName, ".template");
            FileUtils.writeStringToFile(tempfile, templateCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long startTime = System.nanoTime();
        CFGBuilder cfgBuilder = new CFGBuilder(tempfile.getAbsolutePath(), null);
        ArrayList<Section> CFG = cfgBuilder.getSections();
        IntervalAnalysis intervalAnalyzer = new IntervalAnalysis();
        int index=stanfile.lastIndexOf('/');
        intervalAnalyzer.setPath(stanfile.substring(0,index));
        intervalAnalyzer.forwardAnalysis(CFG);
        long endTime = System.nanoTime();
        double duration = (endTime - startTime)/1000000000.0;
        System.out.println("Analysis Time: " + duration);
    }

    public static void main (String[] args) {
        String localDir = "/home/zixinh2/analysis/progs/";
        // String localDir = "/Users/zixin/Documents/uiuc/fall20/analysis/progs/";
        AnalysisRunner.analyzeProgram(localDir, args[0]);
    }
}

package grammar.transformations.util;

import grammar.cfg.CFGBuilder;
import grammar.transformations.Reweighter;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.FileUtils;
import translators.Stan2IRTranslator;
import translators.StanTranslator;

import java.io.File;
import java.io.IOException;


public class TransWriter {


    String tempFileName;
    String suffix = ".tmpT";
    String code;
    ParseTreeWalker walker = new ParseTreeWalker();

    public TransWriter(String stanfile, String standata) {
        Stan2IRTranslator stan2IRTranslator = new Stan2IRTranslator(stanfile, standata);
        tempFileName = stanfile.replace(".stan", "");
        code = stan2IRTranslator.getCode();
        System.out.println("========Stan Code=======");
        System.out.println(code);

    }


    public String getCode() {
        return code;
    }

    public String getStanCode() throws Exception {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        StanTranslator stanTranslator = new StanTranslator();
        stanTranslator.translate((new CFGBuilder(file.getAbsolutePath(), null, false)).getSections());
        return stanTranslator.getCode();
    }

    public void transformObserveToLoop() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        ObserveToLoop observeToLoop = new ObserveToLoop(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(observeToLoop, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
    }

    public void transformSampleToTarget() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        SampleToTarget sampleToTarget = new SampleToTarget(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(sampleToTarget, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
    }

    public void transformReweighter() throws IOException {
        File file = File.createTempFile(tempFileName, suffix);
        FileUtils.writeStringToFile(file, code);
        CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null, false);
        TokenStreamRewriter antlrRewriter = new TokenStreamRewriter(cfgBuilder.parser.getTokenStream());
        Reweighter reweighter = new Reweighter(cfgBuilder, antlrRewriter);
        cfgBuilder.parser.reset();
        walker.walk(reweighter, cfgBuilder.parser.template());
        code = antlrRewriter.getText();
    }
}

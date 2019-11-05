package tests;

import com.sun.org.apache.xpath.internal.axes.FilterExprWalker;
import grammar.AST;
import grammar.Template3Parser;
import grammar.cfg.CFGBuilder;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import grammar.transformations.*;
import org.junit.rules.TemporaryFolder;
import translators.Stan2IRTranslator;
import translators.StanTranslator;
import translators.listeners.CFGWalker;
import grammar.transformations.util.ObserveToLoop;
import utils.Utils;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

public class TestTransformer {

    @Test
    @Ignore
    public void TestNormal2T(){
        CFGBuilder cfgBuilder = new CFGBuilder("/home/zixin/Documents/are/PPVM/templates/basic/basic.template", null, false);

        TransformController transformController = new TransformController(cfgBuilder.getSections());
        try {
            transformController.analyze();
            transformController.transform();
            StanTranslator stanTranslator = new StanTranslator();
            stanTranslator.translate(cfgBuilder.getSections());
            stanTranslator.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestTransformers(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/poisson.template", null, false);

        TransformController transformController = new TransformController(cfgBuilder.getSections());
        try {
            transformController.analyze();
            transformController.transform();
            StanTranslator stanTranslator = new StanTranslator();
            stanTranslator.translate(cfgBuilder.getSections());
            stanTranslator.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void TestUndo(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/basic.template", null, false);

        TransformController transformController = new TransformController(cfgBuilder.getSections());
        try {
            transformController.analyze();
            transformController.transform();
            transformController.undoAll();
            StanTranslator stanTranslator = new StanTranslator();
            stanTranslator.translate(cfgBuilder.getSections());
            stanTranslator.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestUndoOne(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/basic_robust4.template", null, false);

        TransformController transformController = new TransformController(cfgBuilder.getSections());
        try {
            transformController.analyze();
            transformController.transform_one();
            transformController.undo();
            transformController.transform_one();
            StanTranslator stanTranslator = new StanTranslator();
            stanTranslator.translate(cfgBuilder.getSections());
            stanTranslator.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void TestReweighter(){

        try {
            Stan2IRTranslator stan2IRTranslator =
                    new Stan2IRTranslator("src/test/resources/stan/radon.pooling.stan",
                            "src/test/resources/stan/radon.pooling.data.R");
            String code = stan2IRTranslator.getCode();
            System.out.println(code);
            Reweighter reweighter = new Reweighter();
            File file = temporaryFolder.newFile();
            FileUtils.writeStringToFile(file, code);
            System.out.println(code);
            CFGBuilder cfgBuilder = new CFGBuilder(file.getAbsolutePath(), null);
            ObserveToLoop observeToLoop = new ObserveToLoop(cfgBuilder);

            ParseTreeWalker walker = new ParseTreeWalker();
            CFGWalker cfgWalker = new CFGWalker(cfgBuilder.getSections(), observeToLoop);
            cfgWalker.walk();
            Template3Parser parser = cfgBuilder.parser;
            parser.reset();
            walker.walk(observeToLoop, parser.template());
            String templateCode = observeToLoop.antlrRewriter.getText();
            System.out.println(templateCode);

            File transfile = temporaryFolder.newFile();
            FileUtils.writeStringToFile(transfile, templateCode);
            StanTranslator stanTranslator = new StanTranslator();
            stanTranslator.translate((new CFGBuilder(transfile.getAbsolutePath(), null)).getSections());
            System.out.println(stanTranslator.getCode());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    @Test
    public void TestReweighter2(){
        try {
            // Stan2IRTranslator stan2IRTranslator =
            //         new Stan2IRTranslator("src/test/resources/stan/stan1610.stan",
            //                 "src/test/resources/stan/stan1610.data");
            // String code = stan2IRTranslator.getCode();
            // System.out.println(code);
            // // Reweighter reweighter = new Reweighter();
            // File file = temporaryFolder.newFile();
            // FileUtils.writeStringToFile(file, code);
            // System.out.println(code);
            CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/poisson.template", null);

            ObserveToLoop observeToLoop = new ObserveToLoop(cfgBuilder);
            CFGWalker walker = new CFGWalker(cfgBuilder.getSections(), observeToLoop);
            walker.walk();
            //reweighter.availTransformers(cfgBuilder.getSections(), queuedTransformers);
            StanTranslator stanTranslator = new StanTranslator();
            // stanTranslator.translate(observeToLoop.rewriter.rewrite());
            stanTranslator.translate(cfgBuilder.getSections());
            System.out.println(stanTranslator.getCode());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

package tests;

import grammar.cfg.CFGBuilder;
import org.junit.Ignore;
import org.junit.Test;
import grammar.transformations.*;
import translators.StanTranslator;
import utils.Utils;

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
    @Ignore
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
}

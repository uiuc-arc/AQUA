package tests;

import grammar.cfg.CFGBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import translators.Edward2Translator;
import translators.PyroTranslator;

public class TestPyroTranslation {

    @Test
    public void testPyroLR(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/linearregression.template", null, false);
        PyroTranslator pyroTranslator = new PyroTranslator();
        try {
            pyroTranslator.translate(cfgBuilder.getSections());
            System.out.println(pyroTranslator.getCode());
            //Pair results = pyroTranslator.run();
            //Assert.assertTrue(results.getRight().toString().length() == 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPyroMLR(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/multiplelinearregression.template", null, false);
        PyroTranslator pyroTranslator = new PyroTranslator();
        try {
            pyroTranslator.translate(cfgBuilder.getSections());
            System.out.println(pyroTranslator.getCode());
            //Pair results = pyroTranslator.run();
            //Assert.assertTrue(results.getRight().toString().length() == 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPyroCond(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/conditional.template", null, false);
        PyroTranslator pyroTranslator = new PyroTranslator();
        try {
            pyroTranslator.translate(cfgBuilder.getSections());
            System.out.println(pyroTranslator.getCode());
            //Pair results = pyroTranslator.run();
            //Assert.assertTrue(results.getRight().toString().length() == 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPyroContz(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/continualization.template", null, true);
        PyroTranslator pyroTranslator = new PyroTranslator();
        try {
            pyroTranslator.translate(cfgBuilder.getSections());
            System.out.println(pyroTranslator.getCode());
            //Pair results = pyroTranslator.run();
            //Assert.assertTrue(results.getRight().toString().length() == 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

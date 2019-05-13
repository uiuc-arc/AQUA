package tests;

import grammar.cfg.CFGBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import translators.StanTranslator;
import utils.Utils;

public class TestStanTranslation {

    @Test
    public void TestStanTranslation(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/stan2237.template", null, false);

        System.out.println(Utils.STANRUNNER);
        StanTranslator stanTranslator = new StanTranslator();
        try {
            stanTranslator.translate(cfgBuilder.getSections());
            Pair results = stanTranslator.run();
            Assert.assertTrue(results.getRight().toString().length() == 0);
            //System.out.println(stanTranslator.getCode());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestStanTranslation2(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/leukfr0.template", null, true);

        System.out.println(Utils.STANRUNNER);
        StanTranslator stanTranslator = new StanTranslator();
        try {
            stanTranslator.translate(cfgBuilder.getSections());

            Pair results = stanTranslator.run();
            Assert.assertTrue(results.getRight().toString().length() == 0);
            System.out.println(stanTranslator.getCode());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestStanTranslation3(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/leukfr02.template", null, true);

        System.out.println(Utils.STANRUNNER);
        StanTranslator stanTranslator = new StanTranslator();
        try {
            stanTranslator.translate(cfgBuilder.getSections());

            Pair results = stanTranslator.run();
            Assert.assertTrue(results.getRight().toString().length() == 0);
            System.out.println(stanTranslator.getCode());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestStanTranslation4(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/poisson.template", null, true);

        System.out.println(Utils.STANRUNNER);
        StanTranslator stanTranslator = new StanTranslator();
        try {
            stanTranslator.translate(cfgBuilder.getSections());

            Pair results = stanTranslator.run();
            Assert.assertTrue(results.getRight().toString().length() == 0);
            System.out.println(stanTranslator.getCode());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestStanTranslation5(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/psi/probmods/bayes_occams_razor.template", null, true);

        System.out.println(Utils.STANRUNNER);
        StanTranslator stanTranslator = new StanTranslator();
        try {
            stanTranslator.translate(cfgBuilder.getSections());

            Pair results = stanTranslator.run();
            Assert.assertTrue(results.getRight().toString().length() == 0);
            System.out.println(stanTranslator.getCode());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestStanTranslation6(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/eight_schools.template", null, true);

        System.out.println(Utils.STANRUNNER);
        StanTranslator stanTranslator = new StanTranslator();
        try {
            stanTranslator.translate(cfgBuilder.getSections());
            Pair results = stanTranslator.run();
            Assert.assertTrue(results.getRight().toString().length() == 0);
            System.out.println(stanTranslator.getCode());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void TestStanTranslation7(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/binomial.template", null, true);

        System.out.println(Utils.STANRUNNER);
        StanTranslator stanTranslator = new StanTranslator();
        try {
            stanTranslator.translate(cfgBuilder.getSections());
            Pair results = stanTranslator.run();
            Assert.assertTrue(results.getRight().toString().length() == 0);
            System.out.println(stanTranslator.getCode());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

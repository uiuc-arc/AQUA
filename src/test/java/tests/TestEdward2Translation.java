package tests;

import grammar.cfg.CFGBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import translators.Edward2Translator;

public class TestEdward2Translation {

    @Test
    public void TestEdward21(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/linearregression.template", null, false);
        Edward2Translator edward2Translator = new Edward2Translator();
        try {
            edward2Translator.translate(cfgBuilder.getSections());
            System.out.println(edward2Translator.getCode());
            Pair results = edward2Translator.run();
            Assert.assertTrue(results.getRight().toString().length() == 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void TestEdward22(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/multiplelinearregression.template", null, false);
        Edward2Translator edward2Translator = new Edward2Translator();
        try {
            edward2Translator.translate(cfgBuilder.getSections());
            System.out.println(edward2Translator.getCode());
            Pair results = edward2Translator.run();
            Assert.assertTrue(results.getRight().toString().length() == 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

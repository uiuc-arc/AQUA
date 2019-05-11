package tests;

import grammar.cfg.CFGBuilder;
import org.junit.Test;
import translators.Edward2Translator;

public class TestEdward2Translation {

    @Test
    public void TestEdward21(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/linearregression.template", null, false);
        Edward2Translator edward2Translator = new Edward2Translator();
        try {
            edward2Translator.translate(cfgBuilder.getSections());
//            System.out.println(edward2Translator.getDataSection());
//            System.out.println(edward2Translator.getModelCode());
            System.out.println(edward2Translator.getCode());
            edward2Translator.run();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

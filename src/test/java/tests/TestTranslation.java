package tests;

import grammar.cfg.CFGBuilder;
import org.junit.Ignore;
import org.junit.Test;
import translators.StanTranslator;
import utils.Utils;

public class TestTranslation {

    @Test
    @Ignore
    public void TestStanTranslation(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/stan2237.template", null, false);

        System.out.println(Utils.STANRUNNER);
        StanTranslator stanTranslator = new StanTranslator();
        try {
            stanTranslator.translate(cfgBuilder.getSections());
            stanTranslator.run();
            //System.out.println(stanTranslator.getCode());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

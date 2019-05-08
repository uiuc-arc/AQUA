package tests;

import grammar.cfg.CFGBuilder;
import org.junit.Test;
import translators.StanTranslator;

public class TestTranslation {

    @Test
    public void TestStanTranslation(){
        CFGBuilder cfgBuilder = new CFGBuilder("src/test/resources/test3.template", null, false);
        StanTranslator stanTranslator = new StanTranslator();
        try {
            stanTranslator.translate(cfgBuilder.getSections());
            System.out.println(stanTranslator.print());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package tests;

import grammar.cfg.CFGBuilder;
import org.junit.Ignore;
import org.junit.Test;
import grammar.transformations.*;
import translators.StanTranslator;
import utils.Utils;

public class TestTransformer {

    @Test
    public void TestNormal2T(){
        CFGBuilder cfgBuilder = new CFGBuilder("/home/zixin/Documents/are/PPVM/templates/basic/basic.template", null, false);

        Normal2T normal2T = new Normal2T();
        try {
            normal2T.transform(cfgBuilder.getSections());
            StanTranslator stanTranslator = new StanTranslator();
            stanTranslator.translate(cfgBuilder.getSections());
            stanTranslator.run();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

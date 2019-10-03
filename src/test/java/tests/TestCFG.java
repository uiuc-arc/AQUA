package tests;

import grammar.cfg.CFGBuilder;
import org.junit.Test;
import translators.StanTranslator;
import translators.listeners.CFGWalker;
import utils.ObserveRemover;

public class TestCFG {

    @Test
    public void Test1(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/electic_inter.template", "src/test/resources/graph1.png");

    }

    @Test
    public void Test2() {
        CFGBuilder builder = new CFGBuilder("src/test/resources/eight_schools.template", "src/test/resources/graph2.png");
    }

    @Test
    public void Test3() {
        CFGBuilder builder = new CFGBuilder("src/test/resources/stan2237.template", "src/test/resources/graph3.png");
    }

    @Test
    public void TestNestedIfinLoop() {
        CFGBuilder builder = new CFGBuilder("src/test/resources/leukfr02.template", null, true);
    }

    @Test
    public void TestRewriteOp(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/linearregression.template", null, false);
        ObserveRemover observeRemover = new ObserveRemover(builder.getSections());
        CFGWalker walker = new CFGWalker(builder.getSections(), observeRemover);
        walker.walk();
        StanTranslator translator = new StanTranslator();
        try {
            translator.translate(observeRemover.rewriter.rewrite());
            System.out.println(translator.getCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

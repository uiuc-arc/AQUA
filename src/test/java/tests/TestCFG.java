package tests;

import grammar.cfg.CFGBuilder;
import org.junit.Ignore;
import org.junit.Test;

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
}

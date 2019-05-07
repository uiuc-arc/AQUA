package tests;

import grammar.cfg.CFGBuilder;
import org.junit.Test;

public class TestCFG {

    @Test
    public void Test1(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/test1.template", "src/test/resources/graph1.png");

    }

    @Test
    public void Test2() {
        CFGBuilder builder = new CFGBuilder("src/test/resources/test2.template", "src/test/resources/graph2.png");
    }

    @Test
    public void Test3() {
        CFGBuilder builder = new CFGBuilder("src/test/resources/test3.template", "src/test/resources/graph3.png");
    }
}

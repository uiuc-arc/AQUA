package test.java;

import main.java.cfg.CFGBuilder;
import org.junit.Test;

public class TestCFG {

    @Test
    public void Test1(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/test1.template");
    }
}

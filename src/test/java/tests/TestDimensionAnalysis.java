package tests;

import grammar.AST;
import grammar.cfg.CFGBuilder;
import grammar.cfg.Section;
import grammar.cfg.SectionType;
import org.junit.Test;
import utils.Dimension;
import utils.DimensionChecker;
import utils.Types;

public class TestDimensionAnalysis {

    @Test
    public void test1(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/eight_schools.template",
                "src/test/resources/graph2.png");
        Section section = builder.getSections().stream().filter(x -> x.sectionType == SectionType.DATA).findFirst().get();
        Dimension dim = DimensionChecker.getDimension(new AST.Id(section.basicBlocks.get(0).getData().get(1).decl.id.id),
                builder.getSections());
        assert dim.getType() == Types.FLOAT;
        assert dim.getDims().get(0).equals("J");
    }

    @Test
    public void test2(){
        CFGBuilder builder = new CFGBuilder("src/test/resources/basic.template",
                "src/test/resources/graph2.png");
        Section section = builder.getSections().stream().filter(x -> x.sectionType == SectionType.FUNCTION).findFirst().get();
        AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) section.basicBlocks.get(0).getStatements().get(2).statement;
        AST.FunctionCall functionCall = (AST.FunctionCall) assignmentStatement.rhs;

        Dimension dim = DimensionChecker.getDimension(functionCall.parameters.get(0), builder.getSections());
        System.out.println(dim.getType());
        System.out.println(dim.getDims());

    }
}

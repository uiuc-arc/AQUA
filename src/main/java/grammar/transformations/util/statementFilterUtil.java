package grammar.transformations.util;

import grammar.AST;
import grammar.cfg.*;
import grammar.transformations.BaseTransformer;

import java.util.*;

import static grammar.transformations.util.CFGUtil.getNextBlock;

public class statementFilterUtil {
    public static ArrayList<Statement> statementFilterSection(ArrayList<Section> sections, BaseTransformer transformer) throws Exception {

        ArrayList<Statement> statementList = new ArrayList<>();

        for(Section section:sections){
            if(section.sectionType == SectionType.FUNCTION){
                if(section.sectionName.equals("main")){
                    Set<BasicBlock> visited = new HashSet<>();
                    for(BasicBlock basicBlock: section.basicBlocks){
                        BasicBlock curBlock = basicBlock;
                        while(!visited.contains(curBlock)){
                            visited.add(curBlock);
                            if(curBlock.getParent().sectionName.equalsIgnoreCase("main")){
                                if(curBlock.getStatements().size() != 0)
                                    for(Statement statement:curBlock.getStatements()) {
                                        if (transformer.statementFilterFunction(statement))
                                            statementList.add(statement);
                                    }
                            }

                            BasicBlock nextBlock = getNextBlock(curBlock);
                            if(nextBlock != null)
                                curBlock = nextBlock;
                        }
                    }
                }
                else{
                    throw new Exception("Unknown Function!");
                }
            }
        }
        return statementList;
    }


}

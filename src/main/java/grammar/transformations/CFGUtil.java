package grammar.transformations;

import grammar.AST;
import grammar.cfg.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CFGUtil {
    //ArrayList<String>

    public static BasicBlock getNextBlock(BasicBlock curBlock){
        if(curBlock.getEdges().size() > 0){
            // check true edge first
            if(curBlock.getEdges().size() == 1){
                return curBlock.getEdges().get(0).getTarget();
            }
            else{
                String label = curBlock.getEdges().get(0).getLabel();
                if(label != null && label.equalsIgnoreCase("true")){
                    return curBlock.getEdges().get(0).getTarget();
                }
                else{
                    return curBlock.getEdges().get(1).getTarget();
                }
            }
        }
        return null;
    }

    public static boolean isPrior(Statement statement, AST.Expression expression){
        // get id
        String id = null;
        if(expression instanceof AST.Id){
            id = expression.toString();
        }
        else if(expression instanceof AST.ArrayAccess){
            id = ((AST.ArrayAccess) expression) .id.toString();
        }

        if(id != null){
            SymbolInfo info = statement.parent.getSymbolTable().fetch(id);
            if(info != null)
                return info.isPriorVariable();
        }

        return false;
    }

    public static boolean isData(Statement statement, AST.Expression expression){
        // get id
        String id = null;
        if(expression instanceof AST.Id){
            id = expression.toString();
        }
        else if(expression instanceof AST.ArrayAccess){
            id = ((AST.ArrayAccess) expression).id.toString();
        }

        if(id != null){
            SymbolInfo info = statement.parent.getSymbolTable().fetch(id);
            if(info != null)
                return info.isData();
            else{
                System.out.println("Variable not found " +id);
            }
        }

        return false;
    }

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
                                        if (transformer.statementFilter(statement))
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

    /*public static boolean isFreePrior(Statement statement, AST.Expression expression){

    }*/
}

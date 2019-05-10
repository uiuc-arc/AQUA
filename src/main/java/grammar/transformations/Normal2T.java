package grammar.transformations;

import grammar.AST;
import grammar.cfg.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static grammar.transformations.CFGUtil.*;

public class Normal2T implements ITransformer{

    @Override
    public ArrayList<Statement> getAvailTransforms(ArrayList<Section> sections) throws Exception {
        for(Section section:sections){
            if(section.sectionType == SectionType.FUNCTION){
                if(section.sectionName.equals("main")){
                    Set<BasicBlock> visited = new HashSet<>();
                    for(BasicBlock basicBlock: section.basicBlocks){
                        BasicBlock curBlock = basicBlock;
                        while(!visited.contains(curBlock)){
                            visited.add(curBlock);
                            if(curBlock.getParent().sectionName.equalsIgnoreCase("main")){

                                transform_block(curBlock);
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

    }
    private void transform_block(BasicBlock bBlock){
        SymbolTable symbolTable = bBlock.getSymbolTable();
        if(bBlock.getStatements().size() != 0)
            for(Statement statement:bBlock.getStatements()){
                if(statement.statement instanceof AST.AssignmentStatement){
                    AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) statement.statement;
                    if(isPrior(statement, assignmentStatement.lhs) || isData(statement, assignmentStatement.lhs)){
                        if (assignmentStatement.rhs.toString().contains("normal")){
                            if (assignmentStatement.rhs instanceof AST.FunctionCall) {
                                AST.FunctionCall normalDist = ((AST.FunctionCall) assignmentStatement.rhs);
                                normalDist.id = new AST.Id("student_t");
                                normalDist.parameters.add(0,new AST.Integer("1"));
                            }
                        }
                    }
                }
            }
    }

}

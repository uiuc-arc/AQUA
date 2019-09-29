package grammar.transformations;

import grammar.AST;
import grammar.cfg.*;

import java.util.*;

import static grammar.transformations.CFGUtil.*;

public class Normal2T extends BaseTransformer {
    private boolean transformed;
    private AST.FunctionCall normalDist;
    private boolean analysis;

    public Normal2T() {
        transformed = false;
        analysis = false;
    }

    @Override
    public boolean isTransformed() {
        assert !analysis : " Transformer used for analysis!";
        return transformed;
    }

    @Override
    public boolean statementFilter(Statement statement) {
        return false;
    }

    public void addInfo(AST.FunctionCall normalDist){
        assert !analysis : " Transformer used for analysis!";
        this.normalDist = normalDist;
    }


    @Override
    public void transform(){
        assert !analysis : " Transformer used for analysis!";
        System.out.println("Transform Normal2T");
        normalDist.id = new AST.Id("student_t");
        normalDist.parameters.add(0, new AST.Integer("1"));
        transformed = true;
    }

    @Override
    public void undo(){
        assert !analysis : " Transformer used for analysis!";
        System.out.println("Undo Normal2T");
        normalDist.id = new AST.Id("normal");
        normalDist.parameters.remove(0);
        transformed = false;
    }

    @Override
    public void availTransformers(ArrayList<Section> sections, Queue<BaseTransformer> availTrans) throws Exception {
        for(Section section:sections){
            if(section.sectionType == SectionType.FUNCTION){
                if(section.sectionName.equals("main")){
                    Set<BasicBlock> visited = new HashSet<>();
                    for(BasicBlock basicBlock: section.basicBlocks){
                        BasicBlock curBlock = basicBlock;
                        while(!visited.contains(curBlock)){
                            visited.add(curBlock);
                            if(curBlock.getParent().sectionName.equalsIgnoreCase("main")){
                                analyze_block(curBlock, availTrans);
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

    private void analyze_block(BasicBlock bBlock, Queue<BaseTransformer> availTrans){
        analysis = true;
        if(bBlock.getStatements().size() != 0)
            for(Statement statement:bBlock.getStatements()){
                if(statement.statement instanceof AST.AssignmentStatement){
                    AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) statement.statement;
                    if(isPrior(statement, assignmentStatement.lhs) || isData(statement, assignmentStatement.lhs)){
                        if (assignmentStatement.rhs.toString().contains("normal")){
                            if (assignmentStatement.rhs instanceof AST.FunctionCall) {
                                BaseTransformer newTransformer = new Normal2T();
                                ((Normal2T) newTransformer).addInfo(((AST.FunctionCall) assignmentStatement.rhs));
                                availTrans.add(newTransformer);
                            }
                        }
                    }
                }
            }
    }

}

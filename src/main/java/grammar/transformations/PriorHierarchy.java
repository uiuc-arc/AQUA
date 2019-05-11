package grammar.transformations;

import grammar.AST;
import grammar.cfg.BasicBlock;
import grammar.cfg.Section;
import grammar.cfg.SectionType;
import grammar.cfg.Statement;
import translators.StanVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static grammar.transformations.CFGUtil.*;

public class FormerHierarchy extends BaseTransformer{
    @Override
    public void transform() throws Exception {

    }

    @Override
    public void undo() throws Exception {

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
                        output += StanVisitor.evaluate(assignmentStatement.lhs) + "~" + StanVisitor.evaluate(assignmentStatement.rhs) +";\n";
                    }
                    else {
                        output += StanVisitor.evaluate(assignmentStatement.lhs) + "=" + StanVisitor.evaluate(assignmentStatement.rhs) + ";\n";
                    }
                }
                else if(statement.statement instanceof AST.ForLoop){
                    AST.ForLoop loop = (AST.ForLoop) statement.statement;
                    output += "for(" + loop.toString() + ")\n";
                }
                else if(statement.statement instanceof AST.Decl){
                    AST.Decl declaration = (AST.Decl) statement.statement;
                    String declarationString = getDeclarationString(statement, declaration);

                    if(statement.parent.getParent().sectionName.equalsIgnoreCase("main") && isPrior(statement, declaration.id)){
                        this.paramSection+= declarationString;
                    }
                    else{
                        output += declarationString;
                    }
                }
            }
    }

    @Override
    public boolean isTransformed() {
        return false;
    }
}

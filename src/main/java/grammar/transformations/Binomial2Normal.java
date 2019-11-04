package grammar.transformations;

import grammar.AST;
import grammar.cfg.BasicBlock;
import grammar.cfg.Section;
import grammar.cfg.SectionType;
import grammar.cfg.Statement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static grammar.transformations.util.CFGUtil.*;

public class Binomial2Normal extends BaseTransformer {
    private boolean transformed;
    private AST.FunctionCall binomialDist;
    private boolean analysis;

    public Binomial2Normal() {
        transformed = false;
        analysis = false;
    }

    @Override
    public boolean isTransformed() {
        assert !analysis : " Transformer used for analysis!";
        return transformed;
    }

    @Override
    public boolean statementFilterFunction(Statement statement) {
        return false;
    }

    public void addInfo(AST.FunctionCall binomialDist){
        assert !analysis : " Transformer used for analysis!";
        this.binomialDist = binomialDist;
    }


    @Override
    public void transform() throws Exception {
        assert !analysis : " Transformer used for analysis!";
        System.out.println("Transform Binomial2Normal");
        binomialDist.id = new AST.Id("normal");
        AST.Expression N = binomialDist.parameters.get(0);
        AST.Expression p = binomialDist.parameters.get(1);
        AST.MulOp mu = new AST.MulOp(N,p);
        AST.MinusOp oneMinusP = new AST.MinusOp(new AST.Integer("1"),p);
        AST.MulOp sigmaSquared = new AST.MulOp(mu,oneMinusP);
        ArrayList<AST.Expression> params = new ArrayList<>();
        params.add(mu);
        params.add(sigmaSquared);
        binomialDist.parameters = params;
        transformed = true;
    }

    @Override
    public void undo() throws Exception {
        assert !analysis : " Transformer used for analysis!";
        System.out.println("Undo Binomial2Normal");
        binomialDist.id = new AST.Id("binomial");
        binomialDist.parameters.remove(0);
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
                        if (assignmentStatement.rhs.toString().contains("binomial")){
                            if (assignmentStatement.rhs instanceof AST.FunctionCall) {
                                BaseTransformer newTransformer = new Binomial2Normal();
                                ((Binomial2Normal) newTransformer).addInfo(((AST.FunctionCall) assignmentStatement.rhs));
                                availTrans.add(newTransformer);
                            }
                        }
                    }
                }
            }
    }


}

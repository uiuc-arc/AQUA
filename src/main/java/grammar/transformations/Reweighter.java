package grammar.transformations;

import grammar.AST;
import grammar.cfg.BasicBlock;
import grammar.cfg.Edge;
import grammar.cfg.Section;
import grammar.cfg.Statement;
import utils.Utils;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import static grammar.transformations.CFGUtil.*;

public class Reweighter extends BaseTransformer {
    private boolean transformed;
    private boolean analysis;
    private final List<JsonObject> models= Utils.getDistributions(null);
    private Statement currStatement;

    public void addInfo(Statement statement){
        assert !analysis : " Transformer used for analysis!";
        this.currStatement = statement;
    }

    @Override
    public void transform() throws Exception {
        System.out.println("Reweighter transform");
        System.out.println(currStatement.statement.toString());
        String newParam = "robust_weight" + (String.valueOf(new Random().nextLong()).substring(1));
        if (currStatement.statement instanceof AST.AssignmentStatement) {
            AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) currStatement.statement;
            if (assignmentStatement.lhs.toString().equals("target")) {
                if (assignmentStatement.rhs instanceof AST.AddOp) {
                    AST.AddOp targetRhs = (AST.AddOp) assignmentStatement.rhs;
                    targetRhs.op2 = new AST.MulOp(targetRhs.op2, new AST.Id(newParam));
                    // String toweight = (assignmentStatement.rhs.toString().replaceAll(" ", "").split("target\\+")[1]);
                    System.out.println(assignmentStatement.toString());
                }
            }
            else if (isData(currStatement, assignmentStatement.lhs)) {
                if (assignmentStatement.rhs instanceof AST.FunctionCall) {
                    AST.FunctionCall functionCall = (AST.FunctionCall) assignmentStatement.rhs;
                    String newID = null;
                    for (JsonObject model:this.models) {
                        if (functionCall.id.id.contains(model.getString("name"))){
                            if(model.getString("type").equals("C")) {
                                newID = functionCall.id.id + "_lpdf";
                            } else {
                                newID = functionCall.id.id + "_lpmf";
                            }
                            break;
                        }
                    }
                    assert(newID != null);
                    functionCall.id.id = newID;
                    AST.Dims newDim = new AST.Dims();
                    newDim.dims.add(new AST.Id("reweight_i"));
                    functionCall.parameters.add(0, new AST.ArrayAccess(new AST.Id(assignmentStatement.lhs.toString()),
                            newDim));
                    assignmentStatement.lhs = new AST.Id("target");
                    assignmentStatement.rhs = new AST.AddOp(new AST.Id("target"), new AST.MulOp(functionCall,
                            new AST.ArrayAccess(new AST.Id(newParam), newDim)));
                }
                AST.Block newBlock = new AST.Block();
                newBlock.statements.add(assignmentStatement);
                AST.ForLoop newForLoop = new AST.ForLoop(new AST.Id("reweight_i"), new AST.Range(new AST.Integer("1"), new AST.Id("N")), newBlock);
                // newForLoop.block
                currStatement.statement = newForLoop;

                // //move the rest statements to a new block
                // BasicBlock newBB = new BasicBlock();
                // ArrayList<AST.Statement> succs = new ArrayList<>();
                // int ind = currStatement.parent.getStatements().indexOf(currStatement);  //get this statement's index
                // for (int i = ind; i != currStatement.parent.getStatements().size()-1; i++){
                //     Statement succStmt = currStatement.parent.getStatements().get(i+1);
                //     succs.add(succStmt.statement);
                //     newBB.addStatement(succStmt.statement);
                // }
                // for (int i = ind; i != currStatement.parent.getStatements().size()-1; i++){
                //     currStatement.parent.getStatements().remove(i+1);
                // }
                // AST.Block succBlock = new AST.Block();
                // succBlock.statements.addAll(succs);
                // currStatement.parent.getOutgoingEdges();

                // BasicBlock loop_condition_block = new BasicBlock(currStatement.parent.getParent());
                // currStatement.parent.getSymbolTable().fork(loop_condition_block);
                // Edge newedge = new Edge()
                // statement.parent.addEdge(loop_condition_block, null);

                // if(curBlock.statements.size() > 0){
                //     loop_condition_block = createBasicBlock(section);
                //     curBlock.getSymbolTable().fork(loop_condition_block);
                //     addEdge(curBlock, loop_condition_block, null);

                // }
                // //BasicBlock loop_condition_block = createBasicBlock(section);
                // //BasicBlock loop_condition_block = curBlock;
                // loop_condition_block.addStatement(forLoop);

                // //curBlock.statements.add(forLoop);
                // BasicBlock loopbody = new BasicBlock(forLoop.block.statements, statement.parent.getParent(), statement.parent, "true");
                // BasicBlock newblock;
//              //   if (loopbody.statements.size() != 0){
//              //        newblock = createBasicBlock(section);
//              //   }
//              //   el se{
//              //        newblock = loopbody;
//              //   }

                // newblock = createBasicBlock(section);
                // //addEdge(loopbody, newblock, null);
                // addEdge(loop_condition_block, newblock, "false");
                // addEdge(loopbody, loop_condition_block, "back");

                // loop_condition_block.getSymbolTable().fork(newblock);


                // curBlock = newblock;

            }
        }

    }

    @Override
    public void undo() throws Exception {

    }

    @Override
    public boolean isTransformed() {
        return false;
    }

    @Override
    public boolean statementFilter(Statement statement) {
        // if statement is observe
        if (!statement.statement.annotations.isEmpty()){
            for (AST.Annotation annotation: statement.statement.annotations){
                if(annotation.annotationType == AST.AnnotationType.Observe)
                    return true;
            }
        }
        // if statement is assignment to target or data
        if (statement.statement instanceof AST.AssignmentStatement) {
            AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) statement.statement;
            return assignmentStatement.lhs.toString().equals("target") || isData(statement, assignmentStatement.lhs);
        }
        return false;
    }

    @Override
    public void availTransformers(ArrayList<Section> sections, Queue<BaseTransformer> availTrans) throws Exception {
        ArrayList<Statement> statementList = statementFilterSection(sections, this);
        for (Statement statement:statementList) {
            BaseTransformer newTransformer = new Reweighter();
            ((Reweighter) newTransformer).addInfo(statement);
        }

    }
}

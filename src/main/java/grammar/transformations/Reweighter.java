package grammar.transformations;

import grammar.AST;
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

    @Override
    public void transform() throws Exception {

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
            System.out.println("In Reweighter");
            System.out.println(statement.statement.toString());
            String newParam = "robust_weight" + (String.valueOf(new Random().nextLong()).substring(1));
            if (statement.statement instanceof AST.AssignmentStatement) {
                AST.AssignmentStatement assignmentStatement = (AST.AssignmentStatement) statement.statement;
                if (assignmentStatement.lhs.toString().equals("target")) {
                    if (assignmentStatement.rhs instanceof AST.AddOp) {
                        AST.AddOp targetRhs = (AST.AddOp) assignmentStatement.rhs;
                        targetRhs.op2 = new AST.MulOp(targetRhs.op2, new AST.Id(newParam));
                        // String toweight = (assignmentStatement.rhs.toString().replaceAll(" ", "").split("target\\+")[1]);
                        System.out.println(assignmentStatement.toString());

                    }
                }
                else if (isData(statement, assignmentStatement.lhs)) {
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
                        functionCall.parameters.add(0, new AST.Id(assignmentStatement.lhs.toString()));
                        System.out.println(functionCall.toString());


                    }

                }
            }
        }

    }
}

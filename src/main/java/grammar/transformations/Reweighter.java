package grammar.transformations;

import grammar.AST;
import grammar.cfg.Section;
import grammar.cfg.Statement;

import java.util.ArrayList;
import java.util.Queue;

import static grammar.transformations.CFGUtil.*;

public class Reweighter extends BaseTransformer {
    private boolean transformed;
    private boolean analysis;

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
        }

    }
}

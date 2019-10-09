package utils;

import grammar.AST;
import grammar.cfg.Section;
import grammar.cfg.Statement;
import translators.listeners.StatementListener;
import utils.Operation.Rewriter;

import java.util.ArrayList;

public class ObserveRemover implements StatementListener {

    public Rewriter rewriter;

    public ObserveRemover(ArrayList<Section> sections){
        rewriter = new Rewriter(sections);
    }

    @Override
    public void enterAssignmentStatement(Statement statement) {
        if(statement.statement.annotations.size() > 0){
            for(AST.Annotation annotation:statement.statement.annotations){
                if(annotation.annotationType == AST.AnnotationType.Observe)
                    rewriter.delete(statement);
            }
        }
    }

    @Override
    public void enterForLoopStatement(Statement statement) {

    }

    @Override
    public void enterIfStmt(Statement statement) {

    }

    @Override
    public void enterDeclStatement(Statement statement) {

    }

    @Override
    public void enterFunctionCallStatement(Statement statement) {

    }

    @Override
    public void enterData(AST.Data data) {

    }
}

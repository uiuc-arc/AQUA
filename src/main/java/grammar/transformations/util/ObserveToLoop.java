package grammar.transformations.util;

import grammar.AST;
import grammar.Template3Parser;
import grammar.cfg.BasicBlock;
import grammar.cfg.Section;
import grammar.cfg.Statement;
import org.antlr.v4.runtime.CharStreams;
import translators.listeners.StatementListener;
import utils.Operation.Rewriter;
import utils.Utils;

import java.util.ArrayList;

public class ObserveToLoop implements StatementListener {

    public Rewriter rewriter;

    public ObserveToLoop(ArrayList<Section> sections){
        rewriter = new Rewriter(sections);
    }

    @Override
    public void enterAssignmentStatement(Statement statement) {
        if(statement.statement.annotations.size() > 0){
            for(AST.Annotation annotation:statement.statement.annotations){
                String loop = "";
                if(annotation.annotationType == AST.AnnotationType.Observe)
                    loop = "for(i in 1:n){}";
                Template3Parser parser = Utils.readTemplateFromString(CharStreams.fromString(loop));
                AST.Program program = parser.template().value;

                BasicBlock basicBlock = new BasicBlock();
                statement.parent.getSymbolTable().fork(basicBlock);
                basicBlock.addStatement(program.statements.get(0));


                BasicBlock loopBody = new BasicBlock();
                basicBlock.getSymbolTable().fork(loopBody);
                loopBody.addStatement(statement.statement);

                basicBlock.addOutgoingEdge(loopBody, "true");
                basicBlock.addIncomingEdge(loopBody, "back");
                loopBody.addIncomingEdge(basicBlock, "true");
                loopBody.addOutgoingEdge(basicBlock, "back");

                this.rewriter.replace(statement, basicBlock.getStatements().get(0));
//                    Statement loopStatement = new Statement();
//                    rewriter.replace(statement, );
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

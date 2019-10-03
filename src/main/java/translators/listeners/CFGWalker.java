package translators.listeners;

import grammar.AST;
import grammar.cfg.BasicBlock;
import grammar.cfg.Section;
import grammar.cfg.Statement;

import java.util.ArrayList;

public class CFGWalker {
    private ArrayList<Section> sections;
    private CFGBaseListener listener;
    public CFGWalker(ArrayList<Section> sections, CFGBaseListener listener){
        this.sections = sections;
        this.listener = listener;
    }

    public void walk(){
        if(this.listener instanceof StatementListener){
            for(Section section:this.sections){
                for(BasicBlock basicBlock:section.basicBlocks){
                    for(Statement statement:basicBlock.getStatements()){
                        if(statement.statement instanceof AST.AssignmentStatement)
                            ((StatementListener) this.listener).enterAssignmentStatement(statement);
                        else if(statement.statement instanceof AST.FunctionCallStatement)
                            ((StatementListener) this.listener).enterFunctionCallStatement(statement);
                        else if(statement.statement instanceof AST.Decl)
                            ((StatementListener) this.listener).enterDeclStatement(statement);
                        else if(statement.statement instanceof AST.ForLoop)
                            ((StatementListener) this.listener).enterForLoopStatement(statement);
                        else if(statement.statement instanceof AST.IfStmt)
                            ((StatementListener) this.listener).enterIfStmt(statement);
                    }
                }
            }
        }
    }
}

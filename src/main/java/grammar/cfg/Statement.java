package grammar.cfg;

import grammar.AST;

public class Statement {
    public AST.Statement statement;
    public BasicBlock parent;

    public Statement(AST.Statement statement, BasicBlock basicBlock){
        this.statement = statement;
        this.parent = basicBlock;
    }
}

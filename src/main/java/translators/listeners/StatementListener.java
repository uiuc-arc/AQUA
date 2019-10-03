package translators.listeners;

import grammar.cfg.Statement;

public interface StatementListener extends CFGBaseListener{
    void enterAssignmentStatement(Statement statement);

    void enterForLoopStatement(Statement statement);

    void enterIfStmt(Statement statement);

    void enterDeclStatement(Statement statement);

    void enterFunctionCallStatement(Statement statement);
}
